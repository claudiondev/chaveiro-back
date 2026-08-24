package com.chaveiro_abencoado.back.controller;

import com.chaveiro_abencoado.back.dto.AberturaRequest;
import com.chaveiro_abencoado.back.dto.FechamentoResponse;
import com.chaveiro_abencoado.back.dto.MovimentacaoRequest;
import com.chaveiro_abencoado.back.service.CaixaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/caixa")
public class CaixaController {

    private final CaixaService caixaService;

    public CaixaController(CaixaService caixaService) {
        this.caixaService = caixaService;
    }

    @PostMapping("/abertura")
    public ResponseEntity<FechamentoResponse> abrir(@Valid @RequestBody AberturaRequest request,
                                                     Authentication authentication) {
        FechamentoResponse response = caixaService.abrirCaixa(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/hoje")
    public ResponseEntity<FechamentoResponse> consultarHoje() {
        return ResponseEntity.ok(caixaService.consultarHoje());
    }

    // Histórico: consultar caixa de qualquer data
    @GetMapping("/historico")
    public ResponseEntity<FechamentoResponse> consultarHistorico(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(caixaService.consultarPorData(data));
    }

    @PostMapping("/movimentacao")
    public ResponseEntity<Void> registrarMovimentacao(@Valid @RequestBody MovimentacaoRequest request,
                                                       Authentication authentication) {
        caixaService.registrarMovimentacao(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/fechamento")
    public ResponseEntity<FechamentoResponse> fechar(@RequestBody(required = false) Map<String, String> body) {
        String observacao = (body != null) ? body.get("observacao") : null;
        FechamentoResponse response = caixaService.fecharCaixa(observacao);
        return ResponseEntity.ok(response);
    }
}
