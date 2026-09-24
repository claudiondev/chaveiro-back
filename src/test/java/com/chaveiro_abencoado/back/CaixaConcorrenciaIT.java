package com.chaveiro_abencoado.back;

import com.chaveiro_abencoado.back.dto.AberturaRequest;
import com.chaveiro_abencoado.back.dto.MovimentacaoRequest;
import com.chaveiro_abencoado.back.model.StatusFechamento;
import com.chaveiro_abencoado.back.model.TipoMovimentacao;
import com.chaveiro_abencoado.back.model.UserRole;
import com.chaveiro_abencoado.back.model.Usuario;
import com.chaveiro_abencoado.back.repository.FechamentoDiarioRepository;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import com.chaveiro_abencoado.back.service.CaixaService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

// Prova, contra PostgreSQL real, que o bloqueio de linha da Task 6 impede o cenário descrito
// no plano: "o fechamento calcula os totais enquanto um serviço já passou pela verificação de
// caixa aberto, mas ainda não gravou sua entrada". Dispara movimentações e o fechamento ao
// mesmo tempo, em threads de verdade, e confere que o saldo fechado bate exatamente com o que
// foi persistido — nunca perde nem duplica uma entrada.
@Tag("postgres-it")
@SpringBootTest(properties = "spring.datasource.url=jdbc:postgresql://${DB_IT_HOST:localhost}:${DB_IT_PORT:5432}/chaveiro_it_concorrencia")
@ActiveProfiles("postgres-it")
class CaixaConcorrenciaIT {

    private static final int MOVIMENTACOES_CONCORRENTES = 20;
    private static final BigDecimal VALOR_CADA = new BigDecimal("10.00");

    @Autowired
    private CaixaService caixaService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private FechamentoDiarioRepository fechamentoRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void recriarBanco() throws SQLException {
        PostgresItSupport.recriarBanco("chaveiro_it_concorrencia");
    }

    @Test
    void fechamentoConcorrenteComMovimentacoesNuncaCorrompeOSaldo() throws InterruptedException {
        Usuario dono = new Usuario("Dono Teste", "dono-concorrencia@teste.com",
                passwordEncoder.encode("Teste1234"), UserRole.DONO);
        usuarioRepository.save(dono);

        AberturaRequest abertura = new AberturaRequest();
        abertura.setValorAbertura(new BigDecimal("100.00"));
        caixaService.abrirCaixa(abertura, dono.getEmail());

        // MOVIMENTACOES_CONCORRENTES tentativas de registrar entrada + 1 tentativa de fechar,
        // todas largando ao mesmo tempo (CyclicBarrier), competindo pelo lock da mesma linha.
        int totalTarefas = MOVIMENTACOES_CONCORRENTES + 1;
        ExecutorService executor = Executors.newFixedThreadPool(totalTarefas);
        CyclicBarrier largada = new CyclicBarrier(totalTarefas);
        AtomicInteger movimentacoesAceitas = new AtomicInteger();
        AtomicInteger movimentacoesRecusadas = new AtomicInteger();
        List<Future<?>> tarefas = new CopyOnWriteArrayList<>();

        for (int i = 0; i < MOVIMENTACOES_CONCORRENTES; i++) {
            tarefas.add(executor.submit(() -> {
                aguardarLargada(largada);
                MovimentacaoRequest request = new MovimentacaoRequest();
                request.setTipo(TipoMovimentacao.ENTRADA);
                request.setValor(VALOR_CADA);
                request.setDescricao("Entrada concorrente");
                try {
                    caixaService.registrarMovimentacao(request, dono.getEmail());
                    movimentacoesAceitas.incrementAndGet();
                } catch (RuntimeException e) {
                    // Esperado quando o fechamento venceu a corrida pelo lock primeiro
                    movimentacoesRecusadas.incrementAndGet();
                }
            }));
        }
        Future<?> fechamentoFuture = executor.submit(() -> {
            aguardarLargada(largada);
            try {
                caixaService.fecharCaixa("Fechamento concorrente");
            } catch (RuntimeException ignored) {
                // Só ocorreria se todas as movimentações tivessem, impossivelmente, fechado antes
            }
        });
        tarefas.add(fechamentoFuture);

        for (Future<?> tarefa : tarefas) {
            try {
                tarefa.get(30, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException e) {
                fail("Tarefa concorrente não terminou como esperado: " + e);
            }
        }
        executor.shutdown();

        assertEquals(MOVIMENTACOES_CONCORRENTES, movimentacoesAceitas.get() + movimentacoesRecusadas.get(),
                "toda movimentação deveria ou ter sido aceita ou recusada, nunca travar/sumir");

        Long fechamentoId = jdbcTemplate.queryForObject(
                "SELECT id FROM fechamentos_diarios WHERE data = CURRENT_DATE", Long.class);
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM fechamentos_diarios WHERE id = ?", String.class, fechamentoId);
        assertEquals(StatusFechamento.FECHADO.name(), status, "o fechamento deveria ter sido concluído");

        BigDecimal somaReal = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(valor), 0) FROM movimentacoes_caixa WHERE fechamento_diario_id = ? AND tipo = 'ENTRADA'",
                BigDecimal.class, fechamentoId);
        BigDecimal totalEntradasGravado = jdbcTemplate.queryForObject(
                "SELECT total_entradas FROM fechamentos_diarios WHERE id = ?", BigDecimal.class, fechamentoId);

        // A prova central: o total gravado no fechamento bate exatamente com a soma real das
        // movimentações persistidas — nem uma entrada aceita ficou de fora, nem nenhuma foi
        // contada a mais. Sem o lock da Task 6, esses dois números podiam divergir.
        assertEquals(0, somaReal.compareTo(totalEntradasGravado),
                "total_entradas do fechamento deveria bater exatamente com a soma das movimentações gravadas");
        assertEquals(0, new BigDecimal(movimentacoesAceitas.get()).multiply(VALOR_CADA).compareTo(somaReal),
                "soma real deveria corresponder ao número de movimentações que a API aceitou");
    }

    private void aguardarLargada(CyclicBarrier largada) {
        try {
            largada.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
