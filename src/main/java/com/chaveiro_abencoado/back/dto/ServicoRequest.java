package com.chaveiro_abencoado.back.dto;

import com.chaveiro_abencoado.back.model.FormaPagamento;
import com.chaveiro_abencoado.back.model.StatusPagamento;
import com.chaveiro_abencoado.back.validation.LimitesFinanceiros;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
    @Max(value = LimitesFinanceiros.QUANTIDADE_MAXIMA, message = "Quantidade acima do limite permitido")
    private Integer quantidade;

    @NotNull(message = "Forma de pagamento é obrigatória")
    private FormaPagamento formaPagamento;

    @NotNull(message = "Status de pagamento é obrigatório")
    private StatusPagamento statusPagamento = StatusPagamento.PAGO;

    @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres")
    private String observacao;

    private boolean domicilio = false;

    @Size(max = 300, message = "Endereço deve ter no máximo 300 caracteres")
    private String endereco;

    @PositiveOrZero(message = "Taxa de deslocamento não pode ser negativa")
    @Digits(integer = LimitesFinanceiros.VALOR_DIGITOS_INTEIROS,
            fraction = LimitesFinanceiros.VALOR_DIGITOS_FRACAO,
            message = "Taxa de deslocamento inválida")
    private BigDecimal taxaDeslocamento;

    private boolean garantia = false;

    @Size(max = LimitesFinanceiros.METADADOS_TAMANHO_MAXIMO, message = "Metadados excedem o tamanho máximo")
    private String metadados;
}
