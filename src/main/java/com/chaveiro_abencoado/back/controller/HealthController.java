package com.chaveiro_abencoado.back.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Público (sem autenticação) — usado pelo health check do Render e pelo monitor do UptimeRobot
// pra manter a instância acordada no plano free.
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
