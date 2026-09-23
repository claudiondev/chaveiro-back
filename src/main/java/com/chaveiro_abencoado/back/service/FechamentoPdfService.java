package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.FechamentoResponse;
import com.chaveiro_abencoado.back.dto.ServicoResumoDTO;
import com.chaveiro_abencoado.back.model.*;
import com.chaveiro_abencoado.back.repository.MovimentacaoCaixaRepository;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// PDF A4 do fechamento diário, montado com os mesmos dados do comprovante da tela
@Service
public class FechamentoPdfService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final Color MARINHO = new Color(0x10, 0x20, 0x35);
    private static final Color CINZA = new Color(0x6B, 0x72, 0x80);
    private static final Color LINHA = new Color(0xE5, 0xE7, 0xEB);
    private static final Color OURO = new Color(0xF5, 0xB7, 0x31);

    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, MARINHO);
    private static final Font SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 10, CINZA);
    private static final Font SECAO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, MARINHO);
    private static final Font TEXTO = FontFactory.getFont(FontFactory.HELVETICA, 10, MARINHO);
    private static final Font TEXTO_NEGRITO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, MARINHO);
    private static final Font DESTAQUE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, MARINHO);
    private static final Font RODAPE = FontFactory.getFont(FontFactory.HELVETICA, 8, CINZA);

    private static final Map<String, String> ROTULOS = Map.ofEntries(
            Map.entry("DINHEIRO", "Dinheiro"), Map.entry("PIX", "PIX"),
            Map.entry("CARTAO_DEBITO", "Cartão de débito"), Map.entry("CARTAO_CREDITO", "Cartão de crédito"),
            Map.entry("AVULSA", "Entrada avulsa"), Map.entry("ALIMENTACAO", "Alimentação"),
            Map.entry("FORNECEDOR", "Fornecedor"), Map.entry("TAXAS", "Taxas"), Map.entry("OUTROS", "Outros")
    );

    private final CaixaService caixaService;
    private final MovimentacaoCaixaRepository movimentacaoRepository;
    private final UsuarioRepository usuarioRepository;

    public FechamentoPdfService(CaixaService caixaService,
                                MovimentacaoCaixaRepository movimentacaoRepository,
                                UsuarioRepository usuarioRepository) {
        this.caixaService = caixaService;
        this.movimentacaoRepository = movimentacaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public byte[] gerar(LocalDate data, String emailUsuario) {
        FechamentoResponse caixa = caixaService.consultarPorData(data);
        List<MovimentacaoCaixa> movimentacoes = movimentacaoRepository.findByFechamentoDiarioId(caixa.getId());
        String geradoPor = usuarioRepository.findByEmail(emailUsuario).map(Usuario::getNome).orElse(emailUsuario);

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 48, 48, 48, 48);
        PdfWriter.getInstance(documento, saida);
        documento.addTitle("Fechamento " + data.format(DateTimeFormatter.ISO_DATE));
        documento.open();

        cabecalho(documento, caixa);
        resumo(documento, caixa);
        entradasPorForma(documento, movimentacoes);
        servicos(documento, caixa);
        saidas(documento, movimentacoes);
        if (caixa.getObservacao() != null && !caixa.getObservacao().isBlank()) {
            secao(documento, "Observação");
            documento.add(new Paragraph(caixa.getObservacao(), TEXTO));
        }
        rodape(documento, geradoPor);

        documento.close();
        return saida.toByteArray();
    }

    private void cabecalho(Document documento, FechamentoResponse caixa) {
        PdfPTable tabela = tabela(new float[]{3, 1});
        PdfPCell nome = celula(new Phrase("CHAVEIRO ABENÇOADO", TITULO), Element.ALIGN_LEFT);
        PdfPCell status = celula(new Phrase(caixa.getStatus() == StatusFechamento.FECHADO ? "FECHADO" : "PARCIAL",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9,
                        caixa.getStatus() == StatusFechamento.FECHADO ? Color.WHITE : MARINHO)), Element.ALIGN_CENTER);
        status.setBackgroundColor(caixa.getStatus() == StatusFechamento.FECHADO ? MARINHO : OURO);
        status.setVerticalAlignment(Element.ALIGN_MIDDLE);
        status.setPadding(5);
        tabela.addCell(nome);
        tabela.addCell(status);
        documento.add(tabela);

        String dia = caixa.getData().format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", PT_BR));
        documento.add(new Paragraph("Fechamento de caixa — " + dia, SUBTITULO));
        if (caixa.getStatus() != StatusFechamento.FECHADO) {
            documento.add(new Paragraph("Caixa ainda aberto: os valores podem mudar até o fechamento.", SUBTITULO));
        }
        documento.add(separador());
    }

    private void resumo(Document documento, FechamentoResponse caixa) {
        PdfPTable tabela = tabela(new float[]{3, 1});
        linha(tabela, "Saldo de abertura", moeda(caixa.getValorAbertura()), TEXTO);
        linha(tabela, "Entradas", "+ " + moeda(caixa.getTotalEntradas()), TEXTO);
        linha(tabela, "Saídas", "- " + moeda(caixa.getTotalSaidas()), TEXTO);
        PdfPCell rotulo = celula(new Phrase("Saldo final", DESTAQUE), Element.ALIGN_LEFT);
        PdfPCell valor = celula(new Phrase(moeda(caixa.getSaldoFinal()), DESTAQUE), Element.ALIGN_RIGHT);
        for (PdfPCell c : List.of(rotulo, valor)) {
            c.setBorder(Rectangle.TOP);
            c.setBorderColor(LINHA);
            c.setPaddingTop(8);
        }
        tabela.addCell(rotulo);
        tabela.addCell(valor);
        documento.add(tabela);

        PdfPTable contadores = tabela(new float[]{1, 1});
        contadores.setSpacingBefore(10);
        linha(contadores, "Serviços realizados: " + valorOuZero(caixa.getTotalServicos()),
                "Chaves cortadas: " + valorOuZero(caixa.getTotalChaves()), TEXTO_NEGRITO);
        documento.add(contadores);
    }

    private void entradasPorForma(Document documento, List<MovimentacaoCaixa> movimentacoes) {
        // Ordem fixa: formas de pagamento na ordem do enum e avulsa por último
        Map<String, BigDecimal> porForma = new LinkedHashMap<>();
        for (FormaPagamento forma : FormaPagamento.values()) {
            somarEntradas(movimentacoes, forma).ifPresent(total -> porForma.put(forma.name(), total));
        }
        somarEntradas(movimentacoes, null).ifPresent(total -> porForma.put("AVULSA", total));
        secao(documento, "Entradas por forma de pagamento");
        if (porForma.isEmpty()) {
            documento.add(new Paragraph("Nenhuma entrada neste dia.", SUBTITULO));
            return;
        }
        PdfPTable tabela = tabela(new float[]{3, 1});
        porForma.forEach((forma, valor) -> linha(tabela, ROTULOS.getOrDefault(forma, forma), moeda(valor), TEXTO));
        documento.add(tabela);
    }

    private Optional<BigDecimal> somarEntradas(List<MovimentacaoCaixa> movimentacoes, FormaPagamento forma) {
        return movimentacoes.stream()
                .filter(m -> m.getTipo() == TipoMovimentacao.ENTRADA && m.getFormaPagamento() == forma)
                .map(MovimentacaoCaixa::getValor)
                .reduce(BigDecimal::add);
    }

    private void servicos(Document documento, FechamentoResponse caixa) {
        secao(documento, "Serviços realizados");
        List<ServicoResumoDTO> servicos = caixa.getServicos() == null ? List.of() : caixa.getServicos();
        if (servicos.isEmpty()) {
            documento.add(new Paragraph("Nenhum serviço registrado neste dia.", SUBTITULO));
            return;
        }
        PdfPTable tabela = tabela(new float[]{0.6f, 3, 1});
        cabecalhoTabela(tabela, "Qtd", "Serviço", "Valor");
        for (ServicoResumoDTO s : servicos) {
            tabela.addCell(celulaLinha(s.quantidade() + "x", Element.ALIGN_LEFT));
            tabela.addCell(celulaLinha(s.nome(), Element.ALIGN_LEFT));
            tabela.addCell(celulaLinha(moeda(s.valorTotal()), Element.ALIGN_RIGHT));
        }
        documento.add(tabela);
    }

    private void saidas(Document documento, List<MovimentacaoCaixa> movimentacoes) {
        List<MovimentacaoCaixa> saidas = movimentacoes.stream()
                .filter(m -> m.getTipo() == TipoMovimentacao.SAIDA)
                .sorted(Comparator.comparing(MovimentacaoCaixa::getDataHora))
                .toList();
        secao(documento, "Saídas");
        if (saidas.isEmpty()) {
            documento.add(new Paragraph("Nenhuma saída neste dia.", SUBTITULO));
            return;
        }
        PdfPTable tabela = tabela(new float[]{0.7f, 2.5f, 1.2f, 1});
        cabecalhoTabela(tabela, "Hora", "Descrição", "Categoria", "Valor");
        for (MovimentacaoCaixa m : saidas) {
            String categoria = m.getCategoriaSaida() != null ? m.getCategoriaSaida().name() : "OUTROS";
            tabela.addCell(celulaLinha(m.getDataHora().format(DateTimeFormatter.ofPattern("HH:mm")), Element.ALIGN_LEFT));
            tabela.addCell(celulaLinha(m.getDescricao() != null ? m.getDescricao() : "-", Element.ALIGN_LEFT));
            tabela.addCell(celulaLinha(ROTULOS.getOrDefault(categoria, categoria), Element.ALIGN_LEFT));
            tabela.addCell(celulaLinha(moeda(m.getValor()), Element.ALIGN_RIGHT));
        }
        documento.add(tabela);
    }

    private void rodape(Document documento, String geradoPor) {
        documento.add(separador());
        String quando = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));
        documento.add(new Paragraph("Gerado em " + quando + " por " + geradoPor + " · Sistema Chaveiro Abençoado", RODAPE));
    }

    private void secao(Document documento, String titulo) {
        Paragraph p = new Paragraph(titulo.toUpperCase(PT_BR), SECAO);
        p.setSpacingBefore(16);
        p.setSpacingAfter(6);
        documento.add(p);
    }

    private PdfPTable tabela(float[] larguras) {
        PdfPTable tabela = new PdfPTable(larguras);
        tabela.setWidthPercentage(100);
        return tabela;
    }

    private void linha(PdfPTable tabela, String rotulo, String valor, Font fonte) {
        tabela.addCell(celula(new Phrase(rotulo, fonte), Element.ALIGN_LEFT));
        tabela.addCell(celula(new Phrase(valor, fonte), Element.ALIGN_RIGHT));
    }

    private void cabecalhoTabela(PdfPTable tabela, String... titulos) {
        for (int i = 0; i < titulos.length; i++) {
            PdfPCell c = celula(new Phrase(titulos[i], FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, CINZA)),
                    i == titulos.length - 1 ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            c.setBorder(Rectangle.BOTTOM);
            c.setBorderColor(LINHA);
            c.setPaddingBottom(5);
            tabela.addCell(c);
        }
    }

    private PdfPCell celulaLinha(String texto, int alinhamento) {
        PdfPCell c = celula(new Phrase(texto, TEXTO), alinhamento);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(LINHA);
        c.setPaddingTop(5);
        c.setPaddingBottom(5);
        return c;
    }

    private PdfPCell celula(Phrase conteudo, int alinhamento) {
        PdfPCell c = new PdfPCell(conteudo);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(alinhamento);
        c.setPaddingTop(3);
        c.setPaddingBottom(3);
        return c;
    }

    private Element separador() {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(4);
        p.add(new Chunk(new LineSeparator(0.5f, 100, LINHA, Element.ALIGN_CENTER, -4)));
        return p;
    }

    private String moeda(BigDecimal valor) {
        // NumberFormat usa espaço não separável; troca por espaço comum para a fonte padrão
        return NumberFormat.getCurrencyInstance(PT_BR)
                .format(valor != null ? valor : BigDecimal.ZERO)
                .replace(' ', ' ');
    }

    private int valorOuZero(Integer valor) {
        return valor != null ? valor : 0;
    }
}
