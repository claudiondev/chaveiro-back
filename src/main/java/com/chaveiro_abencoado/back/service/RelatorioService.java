package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.DiaResumoDTO;
import com.chaveiro_abencoado.back.dto.FuncionarioResumoDTO;
import com.chaveiro_abencoado.back.dto.RelatorioResponse;
import com.chaveiro_abencoado.back.dto.ServicoResumoDTO;
import com.chaveiro_abencoado.back.model.*;
import com.chaveiro_abencoado.back.repository.FechamentoDiarioRepository;
import com.chaveiro_abencoado.back.repository.MovimentacaoCaixaRepository;
import com.chaveiro_abencoado.back.repository.ServicoRealizadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RelatorioService {

    private static final BigDecimal CEM = new BigDecimal("100");

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

    // Semana de segunda a domingo que contém a data de referência
    public RelatorioResponse relatorioSemanal(LocalDate referencia) {
        LocalDate inicio = referencia.with(DayOfWeek.MONDAY);
        LocalDate fim = inicio.plusDays(6);
        String periodo = inicio.format(DateTimeFormatter.ofPattern("dd/MM")) + " a " +
                          fim.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return gerarRelatorio(periodo, inicio, fim);
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
        RelatorioResponse response = new RelatorioResponse(periodo, dataInicio, dataFim);

        List<FechamentoDiario> fechamentos = fechamentoRepository.findByDataBetween(dataInicio, dataFim);
        if (fechamentos.isEmpty()) {
            response.setPorDia(resumirPorDia(dataInicio, dataFim, Map.of(), List.of(), List.of()));
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

        response.setTotalPendente(servicos.stream()
                .filter(s -> s.getStatusPagamento() == StatusPagamento.PENDENTE)
                .map(ServicoRealizado::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        response.setTotalGarantias((int) servicos.stream().filter(ServicoRealizado::isGarantia).count());

        response.setServicosPorTipo(ServicoResumoDTO.agruparPorTipo(servicos));
        response.setPorFuncionario(resumirPorFuncionario(servicos));

        Map<Long, LocalDate> dataDoCaixa = fechamentos.stream()
                .collect(Collectors.toMap(FechamentoDiario::getId, FechamentoDiario::getData));
        response.setPorDia(resumirPorDia(dataInicio, dataFim, dataDoCaixa, servicos, movimentacoes));

        return response;
    }

    private List<FuncionarioResumoDTO> resumirPorFuncionario(List<ServicoRealizado> servicos) {
        Map<Long, List<ServicoRealizado>> porUsuario = servicos.stream()
                .collect(Collectors.groupingBy(s -> s.getUsuario().getId(), LinkedHashMap::new, Collectors.toList()));

        return porUsuario.values().stream()
                .map(lista -> {
                    Usuario usuario = lista.get(0).getUsuario();
                    BigDecimal faturamento = lista.stream()
                            .filter(s -> !s.isGarantia())
                            .map(ServicoRealizado::getValorTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal percentual = usuario.getPercentualComissao();
                    BigDecimal comissao = percentual == null ? BigDecimal.ZERO
                            : faturamento.multiply(percentual).divide(CEM, 2, RoundingMode.HALF_UP);
                    return new FuncionarioResumoDTO(usuario.getId(), usuario.getNome(), lista.size(),
                            contarChaves(lista), faturamento, percentual, comissao);
                })
                .sorted(Comparator.comparing(FuncionarioResumoDTO::faturamento).reversed())
                .toList();
    }

    // Um item por dia do período, inclusive dias sem caixa, para o gráfico ter eixo contínuo
    private List<DiaResumoDTO> resumirPorDia(LocalDate inicio, LocalDate fim, Map<Long, LocalDate> dataDoCaixa,
                                             List<ServicoRealizado> servicos, List<MovimentacaoCaixa> movimentacoes) {
        Map<LocalDate, List<MovimentacaoCaixa>> movsPorDia = movimentacoes.stream()
                .collect(Collectors.groupingBy(m -> dataDoCaixa.get(m.getFechamentoDiario().getId())));
        Map<LocalDate, Long> servicosPorDia = servicos.stream()
                .collect(Collectors.groupingBy(s -> dataDoCaixa.get(s.getFechamentoDiario().getId()), Collectors.counting()));

        return inicio.datesUntil(fim.plusDays(1))
                .map(dia -> {
                    List<MovimentacaoCaixa> movs = movsPorDia.getOrDefault(dia, List.of());
                    return new DiaResumoDTO(dia, somar(movs, TipoMovimentacao.ENTRADA),
                            somar(movs, TipoMovimentacao.SAIDA), servicosPorDia.getOrDefault(dia, 0L).intValue());
                })
                .toList();
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
