package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.RelatorioResponse;
import com.chaveiro_abencoado.back.model.*;
import com.chaveiro_abencoado.back.repository.FechamentoDiarioRepository;
import com.chaveiro_abencoado.back.repository.MovimentacaoCaixaRepository;
import com.chaveiro_abencoado.back.repository.ServicoRealizadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    private FechamentoDiarioRepository fechamentoRepository;
    @Mock
    private ServicoRealizadoRepository servicoRepository;
    @Mock
    private MovimentacaoCaixaRepository movimentacaoRepository;

    private RelatorioService relatorioService;

    private final LocalDate dia = LocalDate.of(2026, 9, 22);
    private Usuario joao;
    private TipoServico copia;
    private TipoServico carimbo;
    private FechamentoDiario caixa;

    @BeforeEach
    void setUp() {
        relatorioService = new RelatorioService(fechamentoRepository, servicoRepository, movimentacaoRepository);

        joao = new Usuario("João", "joao@email.com", "hash", UserRole.FUNCIONARIO);
        joao.setId(2L);
        copia = new TipoServico("Cópia simples", new BigDecimal("10.00"), CategoriaServico.CHAVE, true);
        carimbo = new TipoServico("Carimbo", new BigDecimal("45.00"), CategoriaServico.CARIMBO, false);

        caixa = new FechamentoDiario(dia, new BigDecimal("100.00"), joao);
        caixa.setId(1L);
    }

    @Test
    void deveUsarMovimentacoesDoCaixaMesmoComTotaisGravadosDesatualizados() {
        // Caixa aberto com totais antigos gravados: o relatório não pode confiar neles
        caixa.setTotalEntradas(new BigDecimal("999.00"));
        caixa.setTotalSaidas(new BigDecimal("999.00"));
        comCaixas(caixa);
        comServicos(servico(copia, 2, "20.00", FormaPagamento.PIX));
        comMovimentacoes(
                entrada("20.00", FormaPagamento.PIX),
                entrada("30.00", null),
                saida("15.00", CategoriaSaida.ALIMENTACAO)
        );

        RelatorioResponse r = relatorioService.relatorioDiario(dia);

        assertEquals(new BigDecimal("50.00"), r.getTotalEntradas());
        assertEquals(new BigDecimal("15.00"), r.getTotalSaidas());
        assertEquals(new BigDecimal("35.00"), r.getSaldoTotal());
    }

    @Test
    void entradasPorFormaDevemFecharComOFaturamento() {
        comCaixas(caixa);
        comServicos();
        comMovimentacoes(
                entrada("20.00", FormaPagamento.PIX),
                entrada("15.00", FormaPagamento.PIX),
                // Fiado de outro dia pago hoje em dinheiro
                entrada("80.00", FormaPagamento.DINHEIRO),
                entrada("30.00", null)
        );

        RelatorioResponse r = relatorioService.relatorioDiario(dia);

        assertEquals(new BigDecimal("35.00"), r.getEntradasPorFormaPagamento().get("PIX"));
        assertEquals(new BigDecimal("80.00"), r.getEntradasPorFormaPagamento().get("DINHEIRO"));
        assertEquals(new BigDecimal("30.00"), r.getEntradasPorFormaPagamento().get("AVULSA"));
        BigDecimal soma = r.getEntradasPorFormaPagamento().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(r.getTotalEntradas(), soma);
    }

    @Test
    void deveContarServicosEChavesPeloVinculoComOCaixa() {
        comCaixas(caixa);
        comServicos(
                servico(copia, 2, "20.00", FormaPagamento.PIX),
                servico(copia, 3, "30.00", FormaPagamento.DINHEIRO),
                servico(carimbo, 1, "45.00", FormaPagamento.PIX)
        );
        comMovimentacoes();

        RelatorioResponse r = relatorioService.relatorioDiario(dia);

        assertEquals(3, r.getTotalServicos());
        assertEquals(5, r.getTotalChaves());
        verify(servicoRepository, never()).findByDataHoraBetween(any(), any());
    }

    @Test
    void saidaSemCategoriaDeveContarComoOutros() {
        comCaixas(caixa);
        comServicos();
        comMovimentacoes(saida("10.00", null), saida("5.00", CategoriaSaida.OUTROS));

        RelatorioResponse r = relatorioService.relatorioDiario(dia);

        assertEquals(new BigDecimal("15.00"), r.getSaidasPorCategoria().get("OUTROS"));
    }

    @Test
    void periodoSemCaixaDeveVirZerado() {
        when(fechamentoRepository.findByDataBetween(dia, dia)).thenReturn(List.of());

        RelatorioResponse r = relatorioService.relatorioDiario(dia);

        assertEquals(BigDecimal.ZERO, r.getTotalEntradas());
        assertEquals(0, r.getTotalServicos());
        assertTrue(r.getEntradasPorFormaPagamento().isEmpty());
        verify(servicoRepository, never()).findByFechamentoDiarioIdIn(any());
    }

    private void comCaixas(FechamentoDiario... caixas) {
        when(fechamentoRepository.findByDataBetween(any(), any())).thenReturn(List.of(caixas));
    }

    private void comServicos(ServicoRealizado... servicos) {
        when(servicoRepository.findByFechamentoDiarioIdIn(any())).thenReturn(List.of(servicos));
    }

    private void comMovimentacoes(MovimentacaoCaixa... movimentacoes) {
        when(movimentacaoRepository.findByFechamentoDiarioIdIn(any())).thenReturn(List.of(movimentacoes));
    }

    private ServicoRealizado servico(TipoServico tipo, int quantidade, String valorTotal, FormaPagamento forma) {
        ServicoRealizado s = new ServicoRealizado();
        s.setTipoServico(tipo);
        s.setUsuario(joao);
        s.setFechamentoDiario(caixa);
        s.setQuantidade(quantidade);
        s.setValorTotal(new BigDecimal(valorTotal));
        s.setFormaPagamento(forma);
        s.setStatusPagamento(StatusPagamento.PAGO);
        return s;
    }

    private MovimentacaoCaixa entrada(String valor, FormaPagamento forma) {
        MovimentacaoCaixa m = movimentacao(TipoMovimentacao.ENTRADA, valor);
        m.setFormaPagamento(forma);
        return m;
    }

    private MovimentacaoCaixa saida(String valor, CategoriaSaida categoria) {
        MovimentacaoCaixa m = movimentacao(TipoMovimentacao.SAIDA, valor);
        m.setCategoriaSaida(categoria);
        return m;
    }

    private MovimentacaoCaixa movimentacao(TipoMovimentacao tipo, String valor) {
        MovimentacaoCaixa m = new MovimentacaoCaixa();
        m.setTipo(tipo);
        m.setValor(new BigDecimal(valor));
        m.setFechamentoDiario(caixa);
        return m;
    }
}
