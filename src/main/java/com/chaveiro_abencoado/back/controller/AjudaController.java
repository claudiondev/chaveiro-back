package com.chaveiro_abencoado.back.controller;

import com.chaveiro_abencoado.back.dto.ProgressoAjudaRequest;
import com.chaveiro_abencoado.back.dto.ProgressoAjudaResponse;
import com.chaveiro_abencoado.back.model.GuiaAjuda;
import com.chaveiro_abencoado.back.service.AjudaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ajuda")
public class AjudaController {

    private final AjudaService ajudaService;

    public AjudaController(AjudaService ajudaService) {
        this.ajudaService = ajudaService;
    }

    @GetMapping("/progressos")
    public ResponseEntity<List<ProgressoAjudaResponse>> listar(Authentication authentication) {
        return ResponseEntity.ok(ajudaService.listar(authentication.getName()));
    }

    @PutMapping("/progressos/{guia}")
    public ResponseEntity<ProgressoAjudaResponse> salvar(@PathVariable GuiaAjuda guia,
                                                         @Valid @RequestBody ProgressoAjudaRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(ajudaService.salvar(guia, request, authentication.getName()));
    }
}
