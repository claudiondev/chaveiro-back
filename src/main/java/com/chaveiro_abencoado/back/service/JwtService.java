package com.chaveiro_abencoado.back.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String gerarToken(String email, String role, int versaoSessao) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("v", versaoSessao)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey)
                .compact();
    }

    public String extrairEmail(String token) {
        return extrairClaims(token).getSubject();
    }

    public String extrairRole(String token) {
        return extrairClaims(token).get("role", String.class);
    }

    // Tokens emitidos antes desta mudança não têm o claim "v"; tratamos como versão 1
    // (a versão inicial de todo usuário), para não derrubar sessões já em uso no deploy.
    public int extrairVersaoSessao(String token) {
        Integer versao = extrairClaims(token).get("v", Integer.class);
        return versao != null ? versao : 1;
    }

    public boolean isTokenValido(String token) {
        try {
            Claims claims = extrairClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
