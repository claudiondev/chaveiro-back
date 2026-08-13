package com.chaveiro_abencoado.back.dto;

import com.chaveiro_abencoado.back.model.CategoriaSaida;
import com.chaveiro_abencoado.back.model.TipoMovimentacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MovimentacaoRequest {

    @NotNull(message = "Tipo é obrigatório")
    private TipoMovimentacao tipo;

    @NotNull(message = "Valor é obrigatório")
    @Positive(message = "Valor deve ser maior que zero")
    private BigDecimal valor;

    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    private String descricao;

    private CategoriaSaida categoriaSaida;
}
