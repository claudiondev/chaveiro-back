package com.chaveiro_abencoado.back.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("chave-secreta-teste-apenas-32-caracteres-minimo!!", 86400000);
    }

    @Test
    void deveGerarTokenValido() {
        String token = jwtService.gerarToken("teste@email.com", "DONO", 1);

        assertNotNull(token);
        assertTrue(jwtService.isTokenValido(token));
    }

    @Test
    void deveExtrairEmailDoToken() {
        String token = jwtService.gerarToken("teste@email.com", "DONO", 1);

        assertEquals("teste@email.com", jwtService.extrairEmail(token));
    }

    @Test
    void deveExtrairRoleDoToken() {
        String token = jwtService.gerarToken("teste@email.com", "FUNCIONARIO", 1);

        assertEquals("FUNCIONARIO", jwtService.extrairRole(token));
    }

    @Test
    void deveExtrairVersaoSessaoDoToken() {
        String token = jwtService.gerarToken("teste@email.com", "DONO", 3);

        assertEquals(3, jwtService.extrairVersaoSessao(token));
    }

    @Test
    void tokenExpiradoDeveSerInvalido() {
        JwtService serviceComExpCurta = new JwtService(
                "chave-secreta-teste-apenas-32-caracteres-minimo!!", -1000);
        String token = serviceComExpCurta.gerarToken("teste@email.com", "DONO", 1);

        assertFalse(jwtService.isTokenValido(token));
    }

    @Test
    void tokenAdulteradoDeveSerInvalido() {
        assertFalse(jwtService.isTokenValido("token.invalido.aqui"));
    }
}
