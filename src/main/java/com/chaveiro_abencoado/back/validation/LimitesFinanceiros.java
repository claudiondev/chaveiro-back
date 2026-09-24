package com.chaveiro_abencoado.back.validation;

import java.math.BigDecimal;

// Limites compartilhados entre validação de entrada (DTOs) e checagem final antes de
// persistir (services). Espelham as colunas DECIMAL(10,2) das migrations — 8 dígitos
// inteiros + 2 decimais. O frontend deve espelhar QUANTIDADE_MAXIMA no formulário de
// registro de serviço, para o usuário ver o limite antes de tentar enviar.
public final class LimitesFinanceiros {

    private LimitesFinanceiros() {}

    // Nenhuma loja registra mais que isso num único atendimento; evita erro de digitação
    // (ex: "1000" em vez de "10") e estouro de valorTotal.
    public static final int QUANTIDADE_MAXIMA = 500;

    public static final int VALOR_DIGITOS_INTEIROS = 8;
    public static final int VALOR_DIGITOS_FRACAO = 2;

    // Maior valor que cabe numa coluna DECIMAL(10,2)
    public static final BigDecimal VALOR_MAXIMO = new BigDecimal("99999999.99");

    public static final int METADADOS_TAMANHO_MAXIMO = 2000;
}
