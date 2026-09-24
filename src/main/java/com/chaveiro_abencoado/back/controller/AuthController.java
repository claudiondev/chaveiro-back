package com.chaveiro_abencoado.back.controller;

import com.chaveiro_abencoado.back.dto.AlterarSenhaRequest;
import com.chaveiro_abencoado.back.dto.CadastroRequest;
import com.chaveiro_abencoado.back.dto.LoginRequest;
import com.chaveiro_abencoado.back.dto.TokenResponse;
import com.chaveiro_abencoado.back.model.Usuario;
import com.chaveiro_abencoado.back.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        // getRemoteAddr() é o endereço da conexão TCP que o servlet container viu — nunca
        // um cabeçalho enviado pelo cliente (X-Forwarded-For não é lido aqui de propósito,
        // porque seria fácil de forjar sem um proxy confiável configurado na frente).
        TokenResponse response = authService.login(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cadastro")
    public ResponseEntity<TokenResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
        TokenResponse response = authService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/senha")
    public ResponseEntity<Map<String, String>> alterarSenha(@Valid @RequestBody AlterarSenhaRequest request,
                                                             Authentication authentication) {
        authService.alterarSenha(
                authentication.getName(),
                request.getSenhaAtual(),
                request.getNovaSenha()
        );
        return ResponseEntity.ok(Map.of("mensagem", "Senha alterada com sucesso"));
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<Map<String, Object>>> listarFuncionarios() {
        List<Map<String, Object>> funcionarios = authService.listarFuncionarios().stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "nome", u.getNome(),
                        "email", u.getEmail(),
                        "ativo", u.isAtivo()
                ))
                .toList();
        return ResponseEntity.ok(funcionarios);
    }

    @PatchMapping("/usuarios/{id}/status")
    public ResponseEntity<Map<String, String>> alterarStatusUsuario(@PathVariable Long id) {
        authService.desativarUsuario(id);
        return ResponseEntity.ok(Map.of("mensagem", "Status alterado com sucesso"));
    }
}
