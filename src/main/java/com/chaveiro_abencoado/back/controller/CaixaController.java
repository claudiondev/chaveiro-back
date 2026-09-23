package com.chaveiro_abencoado.back.controller;

import com.chaveiro_abencoado.back.dto.AberturaRequest;
import com.chaveiro_abencoado.back.dto.FechamentoResponse;
import com.chaveiro_abencoado.back.dto.MovimentacaoRequest;
import com.chaveiro_abencoado.back.dto.PaginaResponse;
import com.chaveiro_abencoado.back.service.CaixaService;
import com.chaveiro_abencoado.back.service.FechamentoPdfService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/caixa")
@Validated
public class CaixaController {

    private final CaixaService caixaService;
    private final FechamentoPdfService fechamentoPdfService;

    public CaixaController(CaixaService caixaService, FechamentoPdfService fechamentoPdfService) {
        this.caixaService = caixaService;
        this.fechamentoPdfService = fechamentoPdfService;
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

    @GetMapping("/historico/pdf")
    public ResponseEntity<byte[]> baixarPdf(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                            Authentication authentication) {
        byte[] pdf = fechamentoPdfService.gerar(data, authentication.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("fechamento-" + data + ".pdf").build().toString())
                .body(pdf);
    }

    @GetMapping("/historico/lista")
    public ResponseEntity<PaginaResponse<FechamentoResponse>> listarHistorico(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return ResponseEntity.ok(PaginaResponse.from(caixaService.listarHistorico(page, size)));
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
