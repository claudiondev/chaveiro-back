package com.chaveiro_abencoado.back.exception;

// Limite de tentativas excedido (login). Mapeada para 429 no GlobalExceptionHandler,
// separada de BusinessException (400) porque o cliente deve tratar de forma diferente:
// esperar e tentar de novo, não corrigir a requisição.
public class RateLimitException extends RuntimeException {
    public RateLimitException(String mensagem) {
        super(mensagem);
    }
}
