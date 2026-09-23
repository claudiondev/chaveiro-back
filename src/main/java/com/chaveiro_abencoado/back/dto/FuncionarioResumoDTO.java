package com.chaveiro_abencoado.back.dto;

import java.math.BigDecimal;

// faturamento = valor dos serviços feitos (sem garantia), pagos ou não
public record FuncionarioResumoDTO(
        Long id,
        String nome,
        int servicos,
        int chaves,
        BigDecimal faturamento,
        BigDecimal percentualComissao,
        BigDecimal comissao
) {
}
