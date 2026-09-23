package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.DiaResumoDTO;
import com.chaveiro_abencoado.back.dto.FuncionarioResumoDTO;
import com.chaveiro_abencoado.back.dto.RelatorioResponse;
import com.chaveiro_abencoado.back.dto.ServicoResumoDTO;
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

    @Test
    void deveResumirPorTipoEFuncionarioComComissao() {
        joao.setPercentualComissao(new BigDecimal("10"));
        Usuario maria = new Usuario("Maria", "maria@email.com", "hash", UserRole.FUNCIONARIO);
        maria.setId(3L);

        ServicoRealizado garantia = servico(copia, 1, "0.00", FormaPagamento.PIX);
        garantia.setGarantia(true);
        ServicoRealizado fiado = servico(carimbo, 1, "45.00", FormaPagamento.PIX);
        fiado.setStatusPagamento(StatusPagamento.PENDENTE);
        ServicoRealizado daMaria = servico(carimbo, 2, "90.00", FormaPagamento.DINHEIRO);
        daMaria.setUsuario(maria);

        comCaixas(caixa);
        comServicos(servico(copia, 3, "30.00", FormaPagamento.PIX), garantia, fiado, daMaria);
        comMovimentacoes();

        RelatorioResponse r = relatorioService.relatorioDiario(dia);

        assertEquals(new BigDecimal("45.00"), r.getTotalPendente());
        assertEquals(1, r.getTotalGarantias());
        assertEquals(List.of(
                new ServicoResumoDTO("Carimbo", 3, new BigDecimal("135.00")),
                new ServicoResumoDTO("Cópia simples", 4, new BigDecimal("30.00"))
        ), r.getServicosPorTipo());

        // Maria faturou mais e vem primeiro; sem percentual, comissão zero
        FuncionarioResumoDTO primeira = r.getPorFuncionario().get(0);
        assertEquals("Maria", primeira.nome());
        assertEquals(0, BigDecimal.ZERO.compareTo(primeira.comissao()));
        assertNull(primeira.percentualComissao());

        // João: garantia conta como serviço e chave, mas não entra no faturamento
        FuncionarioResumoDTO joaoResumo = r.getPorFuncionario().get(1);
        assertEquals(3, joaoResumo.servicos());
        assertEquals(4, joaoResumo.chaves());
        assertEquals(new BigDecimal("75.00"), joaoResumo.faturamento());
        assertEquals(new BigDecimal("7.50"), joaoResumo.comissao());
    }

    @Test
    void semanalDeveIrDeSegundaADomingoComTodosOsDias() {
        // 22/09/2026 é terça; a semana começa na segunda 21/09
        FechamentoDiario sexta = new FechamentoDiario(LocalDate.of(2026, 9, 25), new BigDecimal("50.00"), joao);
        sexta.setId(2L);
        comCaixas(caixa, sexta);
        ServicoRealizado naSexta = servico(copia, 1, "10.00", FormaPagamento.PIX);
        naSexta.setFechamentoDiario(sexta);
        comServicos(servico(copia, 1, "10.00", FormaPagamento.PIX), naSexta);
        MovimentacaoCaixa saidaSexta = saida("7.00", CategoriaSaida.TAXAS);
        saidaSexta.setFechamentoDiario(sexta);
        comMovimentacoes(entrada("10.00", FormaPagamento.PIX), saidaSexta);

        RelatorioResponse r = relatorioService.relatorioSemanal(dia);

        assertEquals(LocalDate.of(2026, 9, 21), r.getDataInicio());
        assertEquals(LocalDate.of(2026, 9, 27), r.getDataFim());
        assertEquals(7, r.getPorDia().size());
        assertEquals(new DiaResumoDTO(LocalDate.of(2026, 9, 21), BigDecimal.ZERO, BigDecimal.ZERO, 0), r.getPorDia().get(0));
        assertEquals(new DiaResumoDTO(dia, new BigDecimal("10.00"), BigDecimal.ZERO, 1), r.getPorDia().get(1));
        assertEquals(new DiaResumoDTO(LocalDate.of(2026, 9, 25), BigDecimal.ZERO, new BigDecimal("7.00"), 1), r.getPorDia().get(4));
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
