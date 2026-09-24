package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.exception.RateLimitException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRateLimiterTest {

    @Test
    void naoBloqueiaAbaixoDoLimite() {
        LoginRateLimiter limitador = new LoginRateLimiter(5, 60_000, 100);
        for (int i = 0; i < 4; i++) {
            limitador.registrarFalha("a@b.com");
        }
        assertDoesNotThrow(() -> limitador.verificarBloqueio("a@b.com"));
    }

    @Test
    void bloqueiaNoLimite() {
        LoginRateLimiter limitador = new LoginRateLimiter(5, 60_000, 100);
        for (int i = 0; i < 5; i++) {
            limitador.registrarFalha("a@b.com");
        }
        assertThrows(RateLimitException.class, () -> limitador.verificarBloqueio("a@b.com"));
    }

    @Test
    void naoAfetaChaveDiferente() {
        LoginRateLimiter limitador = new LoginRateLimiter(5, 60_000, 100);
        for (int i = 0; i < 5; i++) {
            limitador.registrarFalha("a@b.com");
        }
        assertDoesNotThrow(() -> limitador.verificarBloqueio("c@d.com"));
    }

    @Test
    void limparRemoveOBloqueio() {
        LoginRateLimiter limitador = new LoginRateLimiter(5, 60_000, 100);
        for (int i = 0; i < 5; i++) {
            limitador.registrarFalha("a@b.com");
        }
        limitador.limpar("a@b.com");
        assertDoesNotThrow(() -> limitador.verificarBloqueio("a@b.com"));
    }

    @Test
    void entradaExpiraApósAJanela() throws InterruptedException {
        LoginRateLimiter limitador = new LoginRateLimiter(5, 50, 100);
        for (int i = 0; i < 5; i++) {
            limitador.registrarFalha("a@b.com");
        }
        assertThrows(RateLimitException.class, () -> limitador.verificarBloqueio("a@b.com"));

        Thread.sleep(80);

        assertDoesNotThrow(() -> limitador.verificarBloqueio("a@b.com"));
    }

    @Test
    void descartaAsChavesMaisAntigasQuandoPassaDaCapacidadeMaxima() {
        int capacidade = 50;
        LoginRateLimiter limitador = new LoginRateLimiter(5, 60_000, capacidade);

        // Bloqueia a vítima primeiro...
        for (int i = 0; i < 5; i++) {
            limitador.registrarFalha("vitima@email.com");
        }
        assertThrows(RateLimitException.class, () -> limitador.verificarBloqueio("vitima@email.com"));

        // ...depois um atacante usa uma chave nova a cada tentativa, muito além da
        // capacidade. Sem o limite de tamanho, o mapa cresceria sem parar (era o
        // vazamento de memória do limitador antigo).
        for (int i = 0; i < capacidade * 5; i++) {
            limitador.registrarFalha("atacante-" + i + "@qualquer.com");
        }

        // A entrada da vítima foi descartada pelo LRU: o bloqueio dela sumiu junto.
        // (Isso é uma consequência aceitável do limite de memória, não o objetivo —
        // o objetivo é o mapa nunca crescer sem limite.)
        assertDoesNotThrow(() -> limitador.verificarBloqueio("vitima@email.com"));
    }
}
