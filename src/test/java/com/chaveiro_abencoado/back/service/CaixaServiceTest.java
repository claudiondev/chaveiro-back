package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.AberturaRequest;
import com.chaveiro_abencoado.back.dto.FechamentoResponse;
import com.chaveiro_abencoado.back.dto.ServicoResumoDTO;
import com.chaveiro_abencoado.back.model.CategoriaServico;
import com.chaveiro_abencoado.back.model.FechamentoDiario;
import com.chaveiro_abencoado.back.model.ServicoRealizado;
import com.chaveiro_abencoado.back.model.TipoServico;
import com.chaveiro_abencoado.back.model.StatusFechamento;
import com.chaveiro_abencoado.back.model.UserRole;
import com.chaveiro_abencoado.back.model.Usuario;
import com.chaveiro_abencoado.back.repository.FechamentoDiarioRepository;
import com.chaveiro_abencoado.back.repository.MovimentacaoCaixaRepository;
import com.chaveiro_abencoado.back.repository.ServicoRealizadoRepository;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class CaixaServiceTest {

    @Mock
    private FechamentoDiarioRepository fechamentoRepository;
    @Mock
    private MovimentacaoCaixaRepository movimentacaoRepository;
    @Mock
    private ServicoRealizadoRepository servicoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private CaixaService caixaService;

    private Usuario usuario;
    private final TipoServico copia = new TipoServico("Cópia simples", new BigDecimal("10.00"), CategoriaServico.CHAVE, true);
    private final TipoServico fechadura = new TipoServico("Troca de fechadura", new BigDecimal("80.00"), CategoriaServico.FECHADURA, false);

    @BeforeEach
    void setUp() {
        caixaService = new CaixaService(fechamentoRepository, movimentacaoRepository,
                servicoRepository, usuarioRepository);
        usuario = new Usuario("Dono", "dono@email.com", "hash", UserRole.DONO);
        usuario.setId(1L);
    }

    @Test
    void deveAbrirCaixa() {
        when(fechamentoRepository.existsByData(LocalDate.now())).thenReturn(false);
        when(usuarioRepository.findByEmail("dono@email.com")).thenReturn(Optional.of(usuario));
        when(fechamentoRepository.save(any(FechamentoDiario.class))).thenAnswer(invocation -> {
            FechamentoDiario f = invocation.getArgument(0);
            f.setId(1L);
            return f;
        });

        AberturaRequest request = new AberturaRequest();
        request.setValorAbertura(new BigDecimal("200.00"));

        FechamentoResponse response = caixaService.abrirCaixa(request, "dono@email.com");

        assertEquals(new BigDecimal("200.00"), response.getValorAbertura());
        assertEquals(StatusFechamento.ABERTO, response.getStatus());
    }

    @Test
    void deveRejeitarCaixaDuplicado() {
        when(fechamentoRepository.existsByData(LocalDate.now())).thenReturn(true);

        AberturaRequest request = new AberturaRequest();
        request.setValorAbertura(new BigDecimal("200.00"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> caixaService.abrirCaixa(request, "dono@email.com"));
        assertEquals("Já existe um caixa para hoje", ex.getMessage());
    }

    @Test
    void deveFecharCaixa() {
        FechamentoDiario fechamento = new FechamentoDiario(LocalDate.now(),
                new BigDecimal("200.00"), usuario);
        fechamento.setId(1L);

        when(fechamentoRepository.findByDataAndStatus(LocalDate.now(), StatusFechamento.ABERTO))
                .thenReturn(Optional.of(fechamento));
        when(movimentacaoRepository.findByFechamentoDiarioId(1L)).thenReturn(Collections.emptyList());
        when(servicoRepository.findByFechamentoDiarioId(1L)).thenReturn(Collections.emptyList());
        when(fechamentoRepository.save(any(FechamentoDiario.class))).thenReturn(fechamento);

        FechamentoResponse response = caixaService.fecharCaixa("Dia tranquilo");

        assertEquals(StatusFechamento.FECHADO, response.getStatus());
    }

    @Test
    void deveRejeitarFechamentoSemCaixaAberto() {
        when(fechamentoRepository.findByDataAndStatus(LocalDate.now(), StatusFechamento.ABERTO))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> caixaService.fecharCaixa(null));
    }

    @Test
    void deveListarHistoricoFechadoEmPaginas() {
        FechamentoDiario fechamento = new FechamentoDiario(
                LocalDate.of(2026, 9, 22),
                new BigDecimal("100.00"),
                usuario
        );
        fechamento.setId(8L);
        fechamento.setStatus(StatusFechamento.FECHADO);
        fechamento.setTotalServicos(6);
        fechamento.setSaldoFinal(new BigDecimal("280.00"));

        when(fechamentoRepository.findByStatusOrderByDataDescIdDesc(
                eq(StatusFechamento.FECHADO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fechamento)));

        var resultado = caixaService.listarHistorico(0, 10);

        assertEquals(1, resultado.getTotalElements());
        assertEquals(LocalDate.of(2026, 9, 22), resultado.getContent().get(0).getData());
        assertEquals(6, resultado.getContent().get(0).getTotalServicos());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(fechamentoRepository).findByStatusOrderByDataDescIdDesc(
                eq(StatusFechamento.FECHADO), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(10, pageable.getValue().getPageSize());
    }

    @Test
    void deveDetalharServicosPorTipoNoComprovante() {
        LocalDate data = LocalDate.of(2026, 9, 22);
        FechamentoDiario fechamento = fechado(data);

        when(fechamentoRepository.findTopByDataOrderByIdDesc(data)).thenReturn(Optional.of(fechamento));
        when(servicoRepository.findByFechamentoDiarioId(8L)).thenReturn(List.of(
                servico(copia, 2, "20.00"),
                servico(fechadura, 1, "80.00"),
                servico(copia, 3, "30.00")
        ));

        FechamentoResponse resultado = caixaService.consultarPorData(data);

        assertEquals(List.of(
                new ServicoResumoDTO("Troca de fechadura", 1, new BigDecimal("80.00")),
                new ServicoResumoDTO("Cópia simples", 5, new BigDecimal("50.00"))
        ), resultado.getServicos());
    }

    @Test
    void naoDeveRecalcularCaixaFechado() {
        LocalDate data = LocalDate.of(2026, 9, 22);
        FechamentoDiario fechamento = fechado(data);

        when(fechamentoRepository.findTopByDataOrderByIdDesc(data)).thenReturn(Optional.of(fechamento));
        // Serviço extra no banco não pode alterar o que foi fechado
        when(servicoRepository.findByFechamentoDiarioId(8L)).thenReturn(List.of(servico(fechadura, 1, "80.00")));

        FechamentoResponse resultado = caixaService.consultarPorData(data);

        assertEquals(new BigDecimal("280.00"), resultado.getSaldoFinal());
        assertEquals(6, resultado.getTotalServicos());
        assertEquals(4, resultado.getTotalChaves());
        verify(movimentacaoRepository, never()).findByFechamentoDiarioId(any());
        verify(fechamentoRepository, never()).save(any());
    }

    @Test
    void deveContarServicosPeloCaixaQuandoAberto() {
        FechamentoDiario fechamento = new FechamentoDiario(LocalDate.now(), new BigDecimal("100.00"), usuario);
        fechamento.setId(1L);

        when(fechamentoRepository.findByDataAndStatus(LocalDate.now(), StatusFechamento.ABERTO))
                .thenReturn(Optional.of(fechamento));
        when(movimentacaoRepository.findByFechamentoDiarioId(1L)).thenReturn(Collections.emptyList());
        when(servicoRepository.findByFechamentoDiarioId(1L)).thenReturn(List.of(
                servico(copia, 2, "20.00"),
                servico(fechadura, 1, "80.00")
        ));

        FechamentoResponse resultado = caixaService.consultarHoje();

        assertEquals(2, resultado.getTotalServicos());
        assertEquals(2, resultado.getTotalChaves());
        verify(servicoRepository, never()).findByDataHoraBetween(any(), any());
        verify(fechamentoRepository).save(fechamento);
    }

    private FechamentoDiario fechado(LocalDate data) {
        FechamentoDiario fechamento = new FechamentoDiario(data, new BigDecimal("100.00"), usuario);
        fechamento.setId(8L);
        fechamento.setStatus(StatusFechamento.FECHADO);
        fechamento.setTotalEntradas(new BigDecimal("200.00"));
        fechamento.setTotalSaidas(new BigDecimal("20.00"));
        fechamento.setSaldoFinal(new BigDecimal("280.00"));
        fechamento.setTotalServicos(6);
        fechamento.setTotalChaves(4);
        return fechamento;
    }

    private ServicoRealizado servico(TipoServico tipo, int quantidade, String valorTotal) {
        ServicoRealizado s = new ServicoRealizado();
        s.setTipoServico(tipo);
        s.setQuantidade(quantidade);
        s.setValorTotal(new BigDecimal(valorTotal));
        return s;
    }
}
