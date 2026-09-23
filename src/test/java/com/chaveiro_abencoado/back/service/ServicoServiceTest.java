package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.ServicoDTO;
import com.chaveiro_abencoado.back.dto.ServicoRequest;
import com.chaveiro_abencoado.back.exception.BusinessException;
import com.chaveiro_abencoado.back.model.*;
import com.chaveiro_abencoado.back.repository.FechamentoDiarioRepository;
import com.chaveiro_abencoado.back.repository.MovimentacaoCaixaRepository;
import com.chaveiro_abencoado.back.repository.ServicoRealizadoRepository;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

    @Mock
    private ServicoRealizadoRepository servicoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private FechamentoDiarioRepository fechamentoRepository;
    @Mock
    private MovimentacaoCaixaRepository movimentacaoRepository;
    @Mock
    private TipoServicoService tipoServicoService;

    private ServicoService servicoService;
    private Usuario usuario;
    private TipoServico tipoServico;
    private FechamentoDiario caixa;

    @BeforeEach
    void setUp() {
        servicoService = new ServicoService(servicoRepository, tipoServicoService,
                usuarioRepository, fechamentoRepository, movimentacaoRepository);

        usuario = new Usuario("Func", "func@email.com", "hash", UserRole.FUNCIONARIO);
        usuario.setId(1L);

        tipoServico = new TipoServico("Chave simples", new BigDecimal("15.00"),
                CategoriaServico.CHAVE, true);
        tipoServico.setId(1L);

        caixa = new FechamentoDiario(LocalDate.now(), new BigDecimal("200.00"), usuario);
        caixa.setId(1L);
    }

    @Test
    void deveRegistrarServico() {
        when(fechamentoRepository.findByDataAndStatus(LocalDate.now(), StatusFechamento.ABERTO))
                .thenReturn(Optional.of(caixa));
        when(usuarioRepository.findByEmail("func@email.com")).thenReturn(Optional.of(usuario));
        when(tipoServicoService.buscarPorId(1L)).thenReturn(tipoServico);
        when(servicoRepository.save(any(ServicoRealizado.class))).thenAnswer(invocation -> {
            ServicoRealizado s = invocation.getArgument(0);
            s.setId(1L);
            return s;
        });

        ServicoRequest request = criarRequest(1L, 2, FormaPagamento.PIX, false, false);

        ServicoDTO dto = servicoService.registrar(request, "func@email.com");

        assertEquals(new BigDecimal("30.00"), dto.getValorTotal());
        assertEquals("Chave simples", dto.getTipoServicoNome());
        verify(movimentacaoRepository).save(any(MovimentacaoCaixa.class));
    }

    @Test
    void garantiaNaoDeveGerarMovimentacao() {
        when(fechamentoRepository.findByDataAndStatus(LocalDate.now(), StatusFechamento.ABERTO))
                .thenReturn(Optional.of(caixa));
        when(usuarioRepository.findByEmail("func@email.com")).thenReturn(Optional.of(usuario));
        when(tipoServicoService.buscarPorId(1L)).thenReturn(tipoServico);
        when(servicoRepository.save(any(ServicoRealizado.class))).thenAnswer(invocation -> {
            ServicoRealizado s = invocation.getArgument(0);
            s.setId(2L);
            return s;
        });

        ServicoRequest request = criarRequest(1L, 1, FormaPagamento.DINHEIRO, false, true);

        ServicoDTO dto = servicoService.registrar(request, "func@email.com");

        assertEquals(BigDecimal.ZERO, dto.getValorTotal());
        assertTrue(dto.isGarantia());
        verify(movimentacaoRepository, never()).save(any(MovimentacaoCaixa.class));
    }

    @Test
    void domicilioDeveUsarPrecoExternoQuandoDisponivel() {
        tipoServico.setPrecoExterno(new BigDecimal("25.00"));

        when(fechamentoRepository.findByDataAndStatus(LocalDate.now(), StatusFechamento.ABERTO))
                .thenReturn(Optional.of(caixa));
        when(usuarioRepository.findByEmail("func@email.com")).thenReturn(Optional.of(usuario));
        when(tipoServicoService.buscarPorId(1L)).thenReturn(tipoServico);
        when(servicoRepository.save(any(ServicoRealizado.class))).thenAnswer(invocation -> {
            ServicoRealizado s = invocation.getArgument(0);
            s.setId(3L);
            return s;
        });

        ServicoRequest request = criarRequest(1L, 1, FormaPagamento.PIX, true, false);
        request.setTaxaDeslocamento(new BigDecimal("30.00"));

        ServicoDTO dto = servicoService.registrar(request, "func@email.com");

        // 25.00 (externo) + 30.00 (taxa) = 55.00
        assertEquals(new BigDecimal("55.00"), dto.getValorTotal());
        assertTrue(dto.isDomicilio());
    }

    @Test
    void deveRejeitarRegistroSemCaixaAberto() {
        when(fechamentoRepository.findByDataAndStatus(LocalDate.now(), StatusFechamento.ABERTO))
                .thenReturn(Optional.empty());

        ServicoRequest request = criarRequest(1L, 1, FormaPagamento.DINHEIRO, false, false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> servicoService.registrar(request, "func@email.com"));
        assertTrue(ex.getMessage().contains("Caixa não está aberto"));
    }

    @Test
    void deveRejeitarCancelamentoDeOutroDia() {
        ServicoRealizado servico = new ServicoRealizado();
        servico.setId(1L);
        servico.setDataHora(LocalDateTime.now().minusDays(1));

        when(servicoRepository.findById(1L)).thenReturn(Optional.of(servico));

        assertThrows(RuntimeException.class, () -> servicoService.cancelar(1L, "func@email.com"));
    }

    @Test
    void deveRejeitarCancelamentoComCaixaFechado() {
        caixa.setStatus(StatusFechamento.FECHADO);
        ServicoRealizado servico = new ServicoRealizado();
        servico.setId(1L);
        servico.setDataHora(LocalDateTime.now());
        servico.setFechamentoDiario(caixa);

        when(servicoRepository.findById(1L)).thenReturn(Optional.of(servico));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> servicoService.cancelar(1L, "func@email.com"));
        assertEquals("Caixa já fechado; não é possível cancelar o serviço", ex.getMessage());
        verify(movimentacaoRepository, never()).deleteByDescricaoStartingWithAndFechamentoDiarioId(any(), any());
        verify(servicoRepository, never()).delete(any());
    }

    private ServicoRequest criarRequest(Long tipoId, int qtd, FormaPagamento forma,
                                         boolean domicilio, boolean garantia) {
        ServicoRequest request = new ServicoRequest();
        request.setTipoServicoId(tipoId);
        request.setQuantidade(qtd);
        request.setFormaPagamento(forma);
        request.setDomicilio(domicilio);
        request.setGarantia(garantia);
        return request;
    }
}
