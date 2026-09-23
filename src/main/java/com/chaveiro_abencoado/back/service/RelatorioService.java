package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.RelatorioResponse;
import com.chaveiro_abencoado.back.model.*;
import com.chaveiro_abencoado.back.repository.FechamentoDiarioRepository;
import com.chaveiro_abencoado.back.repository.MovimentacaoCaixaRepository;
import com.chaveiro_abencoado.back.repository.ServicoRealizadoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RelatorioService {

    private final FechamentoDiarioRepository fechamentoRepository;
    private final ServicoRealizadoRepository servicoRepository;
    private final MovimentacaoCaixaRepository movimentacaoRepository;

    public RelatorioService(FechamentoDiarioRepository fechamentoRepository,
                            ServicoRealizadoRepository servicoRepository,
                            MovimentacaoCaixaRepository movimentacaoRepository) {
        this.fechamentoRepository = fechamentoRepository;
        this.servicoRepository = servicoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    public RelatorioResponse relatorioDiario(LocalDate data) {
        String periodo = data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return gerarRelatorio(periodo, data, data);
    }

    public RelatorioResponse relatorioSemanal() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioSemana = hoje.with(DayOfWeek.MONDAY);
        String periodo = inicioSemana.format(DateTimeFormatter.ofPattern("dd/MM")) + " a " +
                          hoje.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return gerarRelatorio(periodo, inicioSemana, hoje);
    }

    public RelatorioResponse relatorioMensal(int mes, int ano) {
        LocalDate inicio = LocalDate.of(ano, mes, 1);
        LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());
        String periodo = inicio.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        return gerarRelatorio(periodo, inicio, fim);
    }

    public Map<String, Object> contarChaves(LocalDate inicio, LocalDate fim) {
        List<ServicoRealizado> servicos = servicosDosCaixas(fechamentoRepository.findByDataBetween(inicio, fim));

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("periodo", inicio.format(DateTimeFormatter.ofPattern("dd/MM")) + " a " +
                       fim.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        resultado.put("totalChaves", contarChaves(servicos));
        return resultado;
    }

    // Tudo sai dos caixas do período (vínculo fechamento_diario_id), não do horário dos registros.
    // Entradas e saídas vêm das movimentações, então o caixa aberto de hoje entra sempre atualizado.
    private RelatorioResponse gerarRelatorio(String periodo, LocalDate dataInicio, LocalDate dataFim) {
        RelatorioResponse response = new RelatorioResponse(periodo);

        List<FechamentoDiario> fechamentos = fechamentoRepository.findByDataBetween(dataInicio, dataFim);
        if (fechamentos.isEmpty()) {
            response.setEntradasPorFormaPagamento(Map.of());
            response.setSaidasPorCategoria(Map.of());
            return response;
        }

        List<ServicoRealizado> servicos = servicosDosCaixas(fechamentos);
        List<MovimentacaoCaixa> movimentacoes = movimentacaoRepository.findByFechamentoDiarioIdIn(ids(fechamentos));

        // Entradas por forma de pagamento; a soma fecha com totalEntradas (avulsas em AVULSA)
        Map<String, BigDecimal> porPagamento = new HashMap<>();
        for (MovimentacaoCaixa mov : movimentacoes) {
            if (mov.getTipo() == TipoMovimentacao.ENTRADA) {
                String forma = mov.getFormaPagamento() != null ? mov.getFormaPagamento().name() : "AVULSA";
                porPagamento.merge(forma, mov.getValor(), BigDecimal::add);
            }
        }
        response.setEntradasPorFormaPagamento(porPagamento);

        response.setTotalServicos(servicos.size());
        response.setTotalChaves(contarChaves(servicos));

        BigDecimal totalEntradas = somar(movimentacoes, TipoMovimentacao.ENTRADA);
        BigDecimal totalSaidas = somar(movimentacoes, TipoMovimentacao.SAIDA);
        response.setTotalEntradas(totalEntradas);
        response.setTotalSaidas(totalSaidas);
        response.setSaldoTotal(totalEntradas.subtract(totalSaidas));

        // Saídas por categoria; sem categoria conta como OUTROS para o total fechar
        Map<String, BigDecimal> porCategoria = new HashMap<>();
        for (MovimentacaoCaixa mov : movimentacoes) {
            if (mov.getTipo() == TipoMovimentacao.SAIDA) {
                CategoriaSaida categoria = mov.getCategoriaSaida() != null ? mov.getCategoriaSaida() : CategoriaSaida.OUTROS;
                porCategoria.merge(categoria.name(), mov.getValor(), BigDecimal::add);
            }
        }
        response.setSaidasPorCategoria(porCategoria);

        return response;
    }

    private List<ServicoRealizado> servicosDosCaixas(List<FechamentoDiario> fechamentos) {
        return fechamentos.isEmpty() ? List.of() : servicoRepository.findByFechamentoDiarioIdIn(ids(fechamentos));
    }

    private List<Long> ids(List<FechamentoDiario> fechamentos) {
        return fechamentos.stream().map(FechamentoDiario::getId).toList();
    }

    private int contarChaves(List<ServicoRealizado> servicos) {
        return servicos.stream()
                .filter(s -> s.getTipoServico().isEhChave())
                .mapToInt(ServicoRealizado::getQuantidade)
                .sum();
    }

    private BigDecimal somar(List<MovimentacaoCaixa> movimentacoes, TipoMovimentacao tipo) {
        return movimentacoes.stream()
                .filter(m -> m.getTipo() == tipo)
                .map(MovimentacaoCaixa::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
