package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.FechamentoResponse;
import com.chaveiro_abencoado.back.dto.ServicoResumoDTO;
import com.chaveiro_abencoado.back.model.*;
import com.chaveiro_abencoado.back.repository.MovimentacaoCaixaRepository;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FechamentoPdfServiceTest {

    @Mock
    private CaixaService caixaService;
    @Mock
    private MovimentacaoCaixaRepository movimentacaoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private FechamentoPdfService pdfService;

    private final LocalDate dia = LocalDate.of(2026, 9, 22);

    @BeforeEach
    void setUp() {
        pdfService = new FechamentoPdfService(caixaService, movimentacaoRepository, usuarioRepository);
        Usuario dono = new Usuario("Claudio", "dono@email.com", "hash", UserRole.DONO);
        when(usuarioRepository.findByEmail("dono@email.com")).thenReturn(Optional.of(dono));
    }

    @Test
    void deveGerarPdfComResumoServicosESaidas() throws Exception {
        FechamentoResponse caixa = caixa(StatusFechamento.FECHADO);
        caixa.setServicos(List.of(
                new ServicoResumoDTO("Abertura de porta", 1, new BigDecimal("80.00")),
                new ServicoResumoDTO("Cópia de chave simples", 3, new BigDecimal("30.00"))
        ));
        caixa.setObservacao("Dia tranquilo");
        when(caixaService.consultarPorData(dia)).thenReturn(caixa);
        when(movimentacaoRepository.findByFechamentoDiarioId(8L)).thenReturn(List.of(
                entrada("80.00", FormaPagamento.PIX),
                entrada("30.00", FormaPagamento.DINHEIRO),
                saida("15.00", "Almoço", CategoriaSaida.ALIMENTACAO)
        ));

        byte[] pdf = pdfService.gerar(dia, "dono@email.com");

        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
        String texto = extrairTexto(pdf);
        assertTrue(texto.contains("CHAVEIRO ABENÇOADO"));
        assertTrue(texto.contains("FECHADO"));
        assertTrue(texto.contains("R$ 195,00"), "saldo final");
        assertTrue(texto.contains("Abertura de porta"));
        assertTrue(texto.contains("Cópia de chave simples"));
        assertTrue(texto.contains("Almoço"));
        assertTrue(texto.contains("Alimentação"));
        assertTrue(texto.contains("PIX"));
        assertTrue(texto.contains("Dia tranquilo"));
        assertTrue(texto.contains("por Claudio"));
    }

    @Test
    void deveGerarPdfParcialDeDiaSemMovimento() throws Exception {
        FechamentoResponse caixa = caixa(StatusFechamento.ABERTO);
        caixa.setServicos(List.of());
        when(caixaService.consultarPorData(dia)).thenReturn(caixa);
        when(movimentacaoRepository.findByFechamentoDiarioId(8L)).thenReturn(List.of());

        String texto = extrairTexto(pdfService.gerar(dia, "dono@email.com"));

        assertTrue(texto.contains("PARCIAL"));
        assertTrue(texto.contains("Nenhum serviço registrado neste dia."));
        assertTrue(texto.contains("Nenhuma saída neste dia."));
    }

    private String extrairTexto(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        PdfTextExtractor extrator = new PdfTextExtractor(reader);
        StringBuilder texto = new StringBuilder();
        for (int pagina = 1; pagina <= reader.getNumberOfPages(); pagina++) {
            texto.append(extrator.getTextFromPage(pagina)).append('\n');
        }
        return texto.toString();
    }

    private FechamentoResponse caixa(StatusFechamento status) {
        FechamentoResponse caixa = new FechamentoResponse();
        caixa.setId(8L);
        caixa.setData(dia);
        caixa.setStatus(status);
        caixa.setValorAbertura(new BigDecimal("100.00"));
        caixa.setTotalEntradas(new BigDecimal("110.00"));
        caixa.setTotalSaidas(new BigDecimal("15.00"));
        caixa.setSaldoFinal(new BigDecimal("195.00"));
        caixa.setTotalServicos(2);
        caixa.setTotalChaves(3);
        return caixa;
    }

    private MovimentacaoCaixa entrada(String valor, FormaPagamento forma) {
        MovimentacaoCaixa m = movimentacao(TipoMovimentacao.ENTRADA, valor);
        m.setFormaPagamento(forma);
        return m;
    }

    private MovimentacaoCaixa saida(String valor, String descricao, CategoriaSaida categoria) {
        MovimentacaoCaixa m = movimentacao(TipoMovimentacao.SAIDA, valor);
        m.setDescricao(descricao);
        m.setCategoriaSaida(categoria);
        return m;
    }

    private MovimentacaoCaixa movimentacao(TipoMovimentacao tipo, String valor) {
        MovimentacaoCaixa m = new MovimentacaoCaixa();
        m.setTipo(tipo);
        m.setValor(new BigDecimal(valor));
        m.setDataHora(LocalDateTime.of(2026, 9, 22, 12, 30));
        return m;
    }
}
