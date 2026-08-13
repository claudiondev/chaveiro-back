package com.chaveiro_abencoado.back.controller;

import com.chaveiro_abencoado.back.dto.TipoServicoDTO;
import com.chaveiro_abencoado.back.dto.TipoServicoRequest;
import com.chaveiro_abencoado.back.service.TipoServicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-servico")
public class TipoServicoController {

    private final TipoServicoService tipoServicoService;

    public TipoServicoController(TipoServicoService tipoServicoService) {
        this.tipoServicoService = tipoServicoService;
    }

    @GetMapping
    public ResponseEntity<List<TipoServicoDTO>> listar() {
        return ResponseEntity.ok(tipoServicoService.listarAtivos());
    }

    @PostMapping
    public ResponseEntity<TipoServicoDTO> criar(@Valid @RequestBody TipoServicoRequest request) {
        TipoServicoDTO dto = tipoServicoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoServicoDTO> atualizar(@PathVariable Long id,
                                                     @Valid @RequestBody TipoServicoRequest request) {
        return ResponseEntity.ok(tipoServicoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        tipoServicoService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
