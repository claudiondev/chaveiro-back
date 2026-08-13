package com.chaveiro_abencoado.back.dto;

import com.chaveiro_abencoado.back.model.FormaPagamento;
import com.chaveiro_abencoado.back.model.StatusPagamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServicoRequest {

    @NotNull(message = "Tipo de serviço é obrigatório")
    private Long tipoServicoId;

    @NotNull(message = "Quantidade é obrigatória")
    @Positive(message = "Quantidade deve ser maior que zero")
    private Integer quantidade;

    @NotNull(message = "Forma de pagamento é obrigatória")
    private FormaPagamento formaPagamento;

    private StatusPagamento statusPagamento = StatusPagamento.PAGO;

    @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres")
    private String observacao;

    private boolean domicilio = false;

    @Size(max = 300, message = "Endereço deve ter no máximo 300 caracteres")
    private String endereco;

    private BigDecimal taxaDeslocamento;

    private boolean garantia = false;

    private String metadados;
}
