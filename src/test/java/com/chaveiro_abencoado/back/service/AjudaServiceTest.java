package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.ProgressoAjudaRequest;
import com.chaveiro_abencoado.back.dto.ProgressoAjudaResponse;
import com.chaveiro_abencoado.back.model.GuiaAjuda;
import com.chaveiro_abencoado.back.model.ProgressoAjuda;
import com.chaveiro_abencoado.back.model.StatusProgressoAjuda;
import com.chaveiro_abencoado.back.model.UserRole;
import com.chaveiro_abencoado.back.model.Usuario;
import com.chaveiro_abencoado.back.repository.ProgressoAjudaRepository;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AjudaServiceTest {

    @Mock
    private ProgressoAjudaRepository progressoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private AjudaService ajudaService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        // TransactionTemplate.execute() só precisa de um TransactionStatus para rodar o
        // callback normalmente; commit/rollback são void, o mock já responde como no-op.
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(mock(TransactionStatus.class));
        ajudaService = new AjudaService(progressoRepository, usuarioRepository, transactionManager);
        usuario = new Usuario("Funcionário", "func@email.com", "senha", UserRole.FUNCIONARIO);
        usuario.setId(7L);
        when(usuarioRepository.findByEmail("func@email.com")).thenReturn(Optional.of(usuario));
    }

    @Test
    void deveCriarProgressoParaUsuarioAutenticado() {
        ProgressoAjudaRequest request = request(1, StatusProgressoAjuda.CONCLUIDO);
        when(progressoRepository.findByUsuarioIdAndGuiaParaAtualizar(7L, GuiaAjuda.CAIXA))
                .thenReturn(Optional.empty());
        when(progressoRepository.save(any(ProgressoAjuda.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProgressoAjudaResponse response = ajudaService.salvar(
                GuiaAjuda.CAIXA, request, "func@email.com");

        assertEquals(GuiaAjuda.CAIXA, response.guia());
        assertEquals(1, response.versao());
        assertEquals(StatusProgressoAjuda.CONCLUIDO, response.status());
    }

    @Test
    void deveAtualizarProgressoExistente() {
        ProgressoAjuda progresso = new ProgressoAjuda(usuario, GuiaAjuda.INICIO);
        progresso.setVersao(1);
        progresso.setStatus(StatusProgressoAjuda.DISPENSADO);
        when(progressoRepository.findByUsuarioIdAndGuiaParaAtualizar(7L, GuiaAjuda.INICIO))
                .thenReturn(Optional.of(progresso));
        when(progressoRepository.save(progresso)).thenReturn(progresso);

        ProgressoAjudaResponse response = ajudaService.salvar(
                GuiaAjuda.INICIO, request(2, StatusProgressoAjuda.CONCLUIDO), "func@email.com");

        assertEquals(2, response.versao());
        assertEquals(StatusProgressoAjuda.CONCLUIDO, response.status());
    }

    @Test
    void versaoMaisAntigaNaoSobrescreveAAtual() {
        ProgressoAjuda progresso = new ProgressoAjuda(usuario, GuiaAjuda.MENU);
        progresso.setVersao(2);
        progresso.setStatus(StatusProgressoAjuda.CONCLUIDO);
        when(progressoRepository.findByUsuarioIdAndGuiaParaAtualizar(7L, GuiaAjuda.MENU))
                .thenReturn(Optional.of(progresso));

        ProgressoAjudaResponse response = ajudaService.salvar(
                GuiaAjuda.MENU, request(1, StatusProgressoAjuda.DISPENSADO), "func@email.com");

        // Idempotente: devolve o que já valia, sem erro e sem sobrescrever
        assertEquals(2, response.versao());
        assertEquals(StatusProgressoAjuda.CONCLUIDO, response.status());
        verify(progressoRepository, never()).save(any());
    }

    @Test
    void dispensaDaMesmaVersaoNaoApagaConclusaoJaRegistrada() {
        ProgressoAjuda progresso = new ProgressoAjuda(usuario, GuiaAjuda.CAIXA);
        progresso.setVersao(1);
        progresso.setStatus(StatusProgressoAjuda.CONCLUIDO);
        when(progressoRepository.findByUsuarioIdAndGuiaParaAtualizar(7L, GuiaAjuda.CAIXA))
                .thenReturn(Optional.of(progresso));

        ProgressoAjudaResponse response = ajudaService.salvar(
                GuiaAjuda.CAIXA, request(1, StatusProgressoAjuda.DISPENSADO), "func@email.com");

        assertEquals(StatusProgressoAjuda.CONCLUIDO, response.status(),
                "uma dispensa atrasada da mesma versão não pode apagar uma conclusão já feita");
        verify(progressoRepository, never()).save(any());
    }

    @Test
    void conclusaoDaMesmaVersaoPodeSubstituirUmaDispensaAnterior() {
        ProgressoAjuda progresso = new ProgressoAjuda(usuario, GuiaAjuda.CAIXA);
        progresso.setVersao(1);
        progresso.setStatus(StatusProgressoAjuda.DISPENSADO);
        when(progressoRepository.findByUsuarioIdAndGuiaParaAtualizar(7L, GuiaAjuda.CAIXA))
                .thenReturn(Optional.of(progresso));
        when(progressoRepository.save(progresso)).thenReturn(progresso);

        ProgressoAjudaResponse response = ajudaService.salvar(
                GuiaAjuda.CAIXA, request(1, StatusProgressoAjuda.CONCLUIDO), "func@email.com");

        assertEquals(StatusProgressoAjuda.CONCLUIDO, response.status(),
                "terminar o tour depois de ter dispensado é um avanço legítimo");
    }

    @Test
    void primeiraGravacaoConcorrenteNaoPropagaErroDeConstraint() {
        // Simula a colisão real: a primeira tentativa não acha nada (linha ainda não
        // existe), tenta INSERT e esbarra na constraint única porque outra requisição
        // venceu a corrida. A segunda tentativa (nova transação) já encontra a linha.
        ProgressoAjuda jaSalvoPelaOutraRequisicao = new ProgressoAjuda(usuario, GuiaAjuda.SERVICOS);
        jaSalvoPelaOutraRequisicao.setVersao(1);
        jaSalvoPelaOutraRequisicao.setStatus(StatusProgressoAjuda.CONCLUIDO);

        when(progressoRepository.findByUsuarioIdAndGuiaParaAtualizar(7L, GuiaAjuda.SERVICOS))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(jaSalvoPelaOutraRequisicao));
        when(progressoRepository.save(any(ProgressoAjuda.class)))
                .thenThrow(new DataIntegrityViolationException("uk_progresso_ajuda_usuario_guia"))
                .thenReturn(jaSalvoPelaOutraRequisicao);

        ProgressoAjudaResponse response = ajudaService.salvar(
                GuiaAjuda.SERVICOS, request(1, StatusProgressoAjuda.CONCLUIDO), "func@email.com");

        assertEquals(StatusProgressoAjuda.CONCLUIDO, response.status());
        verify(transactionManager, times(2)).getTransaction(any(TransactionDefinition.class));
    }

    private ProgressoAjudaRequest request(int versao, StatusProgressoAjuda status) {
        ProgressoAjudaRequest request = new ProgressoAjudaRequest();
        request.setVersao(versao);
        request.setStatus(status);
        return request;
    }
}
