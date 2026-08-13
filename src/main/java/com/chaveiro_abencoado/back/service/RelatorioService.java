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
import java.time.LocalDateTime;
import java.time.LocalTime;
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
        LocalDateTime dtInicio = inicio.atStartOfDay();
        LocalDateTime dtFim = fim.atTime(LocalTime.MAX);
        int total = servicoRepository.contarChavesNoPeriodo(dtInicio, dtFim);

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("periodo", inicio.format(DateTimeFormatter.ofPattern("dd/MM")) + " a " +
                       fim.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        resultado.put("totalChaves", total);
        return resultado;
    }

    private RelatorioResponse gerarRelatorio(String periodo, LocalDate dataInicio, LocalDate dataFim) {
        RelatorioResponse response = new RelatorioResponse(periodo);
        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.atTime(LocalTime.MAX);

        List<ServicoRealizado> servicos = servicoRepository.findByDataHoraBetween(inicio, fim);

        // Entradas por forma de pagamento (só serviços pagos e não-garantia)
        Map<String, BigDecimal> porPagamento = new HashMap<>();
        for (ServicoRealizado servico : servicos) {
            if (!servico.isGarantia() && servico.getStatusPagamento() == StatusPagamento.PAGO) {
                String forma = servico.getFormaPagamento().name();
                porPagamento.merge(forma, servico.getValorTotal(), BigDecimal::add);
            }
        }
        response.setEntradasPorFormaPagamento(porPagamento);

        // Totais de serviços
        response.setTotalServicos(servicos.size());
        response.setTotalChaves(servicoRepository.contarChavesNoPeriodo(inicio, fim));

        // Buscar fechamentos do período para entradas e saídas consolidadas
        List<FechamentoDiario> fechamentos = fechamentoRepository.findByDataBetween(dataInicio, dataFim);

        BigDecimal totalEntradas = fechamentos.stream()
                .map(FechamentoDiario::getTotalEntradas)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSaidas = fechamentos.stream()
                .map(FechamentoDiario::getTotalSaidas)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        response.setTotalEntradas(totalEntradas);
        response.setTotalSaidas(totalSaidas);
        response.setSaldoTotal(totalEntradas.subtract(totalSaidas));

        // Saídas por categoria
        Map<String, BigDecimal> porCategoria = new HashMap<>();
        for (FechamentoDiario fechamento : fechamentos) {
            List<MovimentacaoCaixa> movimentacoes = movimentacaoRepository
                    .findByFechamentoDiarioId(fechamento.getId());
            for (MovimentacaoCaixa mov : movimentacoes) {
                if (mov.getTipo() == TipoMovimentacao.SAIDA && mov.getCategoriaSaida() != null) {
                    porCategoria.merge(mov.getCategoriaSaida().name(), mov.getValor(), BigDecimal::add);
                }
            }
        }
        response.setSaidasPorCategoria(porCategoria);

        return response;
    }
}
