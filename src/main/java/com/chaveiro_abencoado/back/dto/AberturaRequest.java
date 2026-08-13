package com.chaveiro_abencoado.back.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AberturaRequest {

    @NotNull(message = "Valor de abertura é obrigatório")
    @PositiveOrZero(message = "Valor de abertura não pode ser negativo")
    private BigDecimal valorAbertura;
}
