package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.CadastroRequest;
import com.chaveiro_abencoado.back.dto.LoginRequest;
import com.chaveiro_abencoado.back.dto.TokenResponse;
import com.chaveiro_abencoado.back.model.UserRole;
import com.chaveiro_abencoado.back.model.Usuario;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public TokenResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciais inválidas"));

        if (!usuario.isAtivo()) {
            throw new RuntimeException("Usuário desativado");
        }

        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            throw new RuntimeException("Credenciais inválidas");
        }

        String token = jwtService.gerarToken(usuario.getEmail(), usuario.getRole().name());
        return new TokenResponse(token, usuario.getRole().name(), usuario.getNome());
    }

    public TokenResponse cadastrar(CadastroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("E-mail já cadastrado");
        }

        Usuario usuario = new Usuario(
                request.getNome(),
                request.getEmail(),
                passwordEncoder.encode(request.getSenha()),
                UserRole.FUNCIONARIO
        );

        usuarioRepository.save(usuario);

        String token = jwtService.gerarToken(usuario.getEmail(), usuario.getRole().name());
        return new TokenResponse(token, usuario.getRole().name(), usuario.getNome());
    }
}
