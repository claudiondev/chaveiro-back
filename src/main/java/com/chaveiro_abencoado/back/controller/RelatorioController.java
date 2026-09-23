package com.chaveiro_abencoado.back.controller;

import com.chaveiro_abencoado.back.dto.RelatorioResponse;
import com.chaveiro_abencoado.back.service.RelatorioService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/diario")
    public ResponseEntity<RelatorioResponse> diario(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        LocalDate filtro = data != null ? data : LocalDate.now();
        return ResponseEntity.ok(relatorioService.relatorioDiario(filtro));
    }

    // inicio: qualquer dia da semana desejada (padrão: semana atual)
    @GetMapping("/semanal")
    public ResponseEntity<RelatorioResponse> semanal(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio) {
        return ResponseEntity.ok(relatorioService.relatorioSemanal(inicio != null ? inicio : LocalDate.now()));
    }

    @GetMapping("/mensal")
    public ResponseEntity<RelatorioResponse> mensal(@RequestParam @Min(1) @Max(12) int mes,
                                                    @RequestParam @Min(2000) @Max(2100) int ano) {
        return ResponseEntity.ok(relatorioService.relatorioMensal(mes, ano));
    }

    @GetMapping("/chaves")
    public ResponseEntity<Map<String, Object>> chaves(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(relatorioService.contarChaves(inicio, fim));
    }
}
