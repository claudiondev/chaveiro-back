package com.chaveiro_abencoado.back.controller;

import com.chaveiro_abencoado.back.dto.ServicoDTO;
import com.chaveiro_abencoado.back.dto.ServicoRequest;
import com.chaveiro_abencoado.back.service.ServicoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    private final ServicoService servicoService;

    public ServicoController(ServicoService servicoService) {
        this.servicoService = servicoService;
    }

    @PostMapping
    public ResponseEntity<ServicoDTO> registrar(@Valid @RequestBody ServicoRequest request,
                                                 Authentication authentication) {
        ServicoDTO dto = servicoService.registrar(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public ResponseEntity<List<ServicoDTO>> listarPorData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        LocalDate filtro = data != null ? data : LocalDate.now();
        return ResponseEntity.ok(servicoService.listarPorData(filtro));
    }

    // Contas a receber
    @GetMapping("/pendentes")
    public ResponseEntity<List<ServicoDTO>> listarPendentes() {
        return ResponseEntity.ok(servicoService.listarPendentes());
    }

    // Marcar como pago
    @PatchMapping("/{id}/pagar")
    public ResponseEntity<ServicoDTO> marcarComoPago(@PathVariable Long id,
                                                      Authentication authentication) {
        return ResponseEntity.ok(servicoService.marcarComoPago(id, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id, Authentication authentication) {
        servicoService.cancelar(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
