package com.chaveiro_abencoado.back.dto;

import com.chaveiro_abencoado.back.model.CategoriaServico;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TipoServicoRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    private String descricao;

    @NotNull(message = "Preço é obrigatório")
    @Positive(message = "Preço deve ser maior que zero")
    private BigDecimal preco;

    @Positive(message = "Preço externo deve ser maior que zero")
    private BigDecimal precoExterno;

    @NotNull(message = "Categoria é obrigatória")
    private CategoriaServico categoria;

    private boolean ehChave = false;
}
