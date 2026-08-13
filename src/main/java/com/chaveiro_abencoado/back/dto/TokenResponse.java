package com.chaveiro_abencoado.back.dto;

import lombok.Data;

@Data
public class TokenResponse {

    private String token;
    private String role;
    private String nome;

    public TokenResponse(String token, String role, String nome) {
        this.token = token;
        this.role = role;
        this.nome = nome;
    }
}
