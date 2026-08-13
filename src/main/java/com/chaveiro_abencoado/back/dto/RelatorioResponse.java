package com.chaveiro_abencoado.back.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class RelatorioResponse {

    private String periodo;
    private BigDecimal totalEntradas;
    private BigDecimal totalSaidas;
    private BigDecimal saldoTotal;
    private Integer totalServicos;
    private Integer totalChaves;
    private Map<String, BigDecimal> entradasPorFormaPagamento;
    private Map<String, BigDecimal> saidasPorCategoria;

    public RelatorioResponse(String periodo) {
        this.periodo = periodo;
        this.totalEntradas = BigDecimal.ZERO;
        this.totalSaidas = BigDecimal.ZERO;
        this.saldoTotal = BigDecimal.ZERO;
        this.totalServicos = 0;
        this.totalChaves = 0;
    }
}
