package com.chaveiro_abencoado.back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DiaResumoDTO(
        LocalDate data,
        BigDecimal entradas,
        BigDecimal saidas,
        int servicos
) {
}
