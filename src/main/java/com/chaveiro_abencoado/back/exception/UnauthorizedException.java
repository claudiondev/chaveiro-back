package com.chaveiro_abencoado.back.exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String mensagem) {
        super(mensagem);
    }
}
