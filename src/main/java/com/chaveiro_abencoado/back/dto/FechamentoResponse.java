package com.chaveiro_abencoado.back.dto;

import com.chaveiro_abencoado.back.model.FechamentoDiario;
import com.chaveiro_abencoado.back.model.StatusFechamento;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FechamentoResponse {

    private Long id;
    private LocalDate data;
    private BigDecimal valorAbertura;
    private BigDecimal totalEntradas;
    private BigDecimal totalSaidas;
    private BigDecimal saldoFinal;
    private Integer totalServicos;
    private Integer totalChaves;
    private StatusFechamento status;
    private String observacao;

    public static FechamentoResponse fromEntity(FechamentoDiario entity) {
        FechamentoResponse dto = new FechamentoResponse();
        dto.setId(entity.getId());
        dto.setData(entity.getData());
        dto.setValorAbertura(entity.getValorAbertura());
        dto.setTotalEntradas(entity.getTotalEntradas());
        dto.setTotalSaidas(entity.getTotalSaidas());
        dto.setSaldoFinal(entity.getSaldoFinal());
        dto.setTotalServicos(entity.getTotalServicos());
        dto.setTotalChaves(entity.getTotalChaves());
        dto.setStatus(entity.getStatus());
        dto.setObservacao(entity.getObservacao());
        return dto;
    }
}
