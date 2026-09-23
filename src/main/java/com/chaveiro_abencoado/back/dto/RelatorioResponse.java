package com.chaveiro_abencoado.back.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class RelatorioResponse {

    private String periodo;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private BigDecimal totalEntradas;
    private BigDecimal totalSaidas;
    private BigDecimal saldoTotal;
    private Integer totalServicos;
    private Integer totalChaves;
    private Map<String, BigDecimal> entradasPorFormaPagamento;
    private Map<String, BigDecimal> saidasPorCategoria;
    // Fiado ainda não pago, gerado por serviços do período
    private BigDecimal totalPendente;
    private Integer totalGarantias;
    private List<ServicoResumoDTO> servicosPorTipo;
    private List<FuncionarioResumoDTO> porFuncionario;
    private List<DiaResumoDTO> porDia;

    public RelatorioResponse(String periodo, LocalDate dataInicio, LocalDate dataFim) {
        this.periodo = periodo;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.totalEntradas = BigDecimal.ZERO;
        this.totalSaidas = BigDecimal.ZERO;
        this.saldoTotal = BigDecimal.ZERO;
        this.totalServicos = 0;
        this.totalChaves = 0;
        this.totalPendente = BigDecimal.ZERO;
        this.totalGarantias = 0;
        this.entradasPorFormaPagamento = Map.of();
        this.saidasPorCategoria = Map.of();
        this.servicosPorTipo = List.of();
        this.porFuncionario = List.of();
        this.porDia = List.of();
    }
}
