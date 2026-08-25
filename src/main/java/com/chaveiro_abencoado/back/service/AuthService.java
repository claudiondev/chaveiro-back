package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.CadastroRequest;
import com.chaveiro_abencoado.back.dto.LoginRequest;
import com.chaveiro_abencoado.back.dto.TokenResponse;
import com.chaveiro_abencoado.back.exception.BusinessException;
import com.chaveiro_abencoado.back.exception.NotFoundException;
import com.chaveiro_abencoado.back.exception.UnauthorizedException;
import com.chaveiro_abencoado.back.model.UserRole;
import com.chaveiro_abencoado.back.model.Usuario;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    // Rate limiting: email -> {tentativas, timestamp do primeiro erro}
    private final Map<String, long[]> tentativasLogin = new ConcurrentHashMap<>();
    private static final int MAX_TENTATIVAS = 5;
    private static final long BLOQUEIO_MS = 15 * 60 * 1000; // 15 minutos

    public AuthService(UsuarioRepository usuarioRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public TokenResponse login(LoginRequest request) {
        verificarBloqueio(request.getEmail());

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    registrarTentativaFalha(request.getEmail());
                    return new UnauthorizedException("Credenciais inválidas");
                });

        if (!usuario.isAtivo()) {
            throw new UnauthorizedException("Usuário desativado");
        }

        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            registrarTentativaFalha(request.getEmail());
            throw new UnauthorizedException("Credenciais inválidas");
        }

        // Login OK, limpa tentativas
        tentativasLogin.remove(request.getEmail());

        String token = jwtService.gerarToken(usuario.getEmail(), usuario.getRole().name());
        return new TokenResponse(token, usuario.getRole().name(), usuario.getNome());
    }

    public TokenResponse cadastrar(CadastroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("E-mail já cadastrado");
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

    public void alterarSenha(String emailUsuario, String senhaAtual, String novaSenha) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new UnauthorizedException("Senha atual incorreta");
        }

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }

    public List<Usuario> listarFuncionarios() {
        return usuarioRepository.findByRole(UserRole.FUNCIONARIO);
    }

    public void desativarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        usuario.setAtivo(!usuario.isAtivo());
        usuarioRepository.save(usuario);
    }

    private void verificarBloqueio(String email) {
        // Limpar entradas expiradas para evitar memory leak
        long agora = System.currentTimeMillis();
        tentativasLogin.entrySet().removeIf(entry -> {
            long[] d = entry.getValue();
            return d[0] >= MAX_TENTATIVAS && (agora - d[1]) >= BLOQUEIO_MS;
        });

        long[] dados = tentativasLogin.get(email);
        if (dados != null && dados[0] >= MAX_TENTATIVAS) {
            long tempoDecorrido = agora - dados[1];
            if (tempoDecorrido < BLOQUEIO_MS) {
                long minutosRestantes = (BLOQUEIO_MS - tempoDecorrido) / 60000 + 1;
                throw new BusinessException("Muitas tentativas. Tente novamente em " + minutosRestantes + " minuto(s)");
            }
            tentativasLogin.remove(email);
        }
    }

    private void registrarTentativaFalha(String email) {
        tentativasLogin.compute(email, (k, dados) -> {
            if (dados == null) {
                return new long[]{1, System.currentTimeMillis()};
            }
            dados[0]++;
            return dados;
        });
    }
}
