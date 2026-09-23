package com.chaveiro_abencoado.back.dto;

import java.math.BigDecimal;

public record ServicoResumoDTO(
        String nome,
        int quantidade,
        BigDecimal valorTotal
) {
}
