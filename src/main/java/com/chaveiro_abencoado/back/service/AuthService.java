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

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_TENTATIVAS = 5;
    private static final long JANELA_MS = 15 * 60 * 1000; // 15 minutos
    private static final int CAPACIDADE_MAXIMA_POR_LIMITADOR = 5000;

    // Dois limitadores independentes: um ataque com muitos e-mails da mesma origem não
    // esgota o limite de contas legítimas, e vice-versa. Cada um é limitado em tamanho
    // (LRU) e nunca faz varredura completa — ver LoginRateLimiter.
    private final LoginRateLimiter limitePorEmail =
            new LoginRateLimiter(MAX_TENTATIVAS, JANELA_MS, CAPACIDADE_MAXIMA_POR_LIMITADOR);
    private final LoginRateLimiter limitePorOrigem =
            new LoginRateLimiter(MAX_TENTATIVAS, JANELA_MS, CAPACIDADE_MAXIMA_POR_LIMITADOR);

    public AuthService(UsuarioRepository usuarioRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    // origemRequisicao: endereço de origem já resolvido pelo controller a partir da
    // conexão TCP (nunca de um cabeçalho enviado pelo cliente) — ver AuthController.
    public TokenResponse login(LoginRequest request, String origemRequisicao) {
        String email = request.getEmail();
        limitePorEmail.verificarBloqueio(email);
        limitePorOrigem.verificarBloqueio(origemRequisicao);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    registrarTentativaFalha(email, origemRequisicao);
                    return new UnauthorizedException("Credenciais inválidas");
                });

        if (!usuario.isAtivo()) {
            throw new UnauthorizedException("Usuário desativado");
        }

        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            registrarTentativaFalha(email, origemRequisicao);
            throw new UnauthorizedException("Credenciais inválidas");
        }

        // Login OK, limpa tentativas dos dois limitadores
        limitePorEmail.limpar(email);
        limitePorOrigem.limpar(origemRequisicao);

        String token = jwtService.gerarToken(usuario.getEmail(), usuario.getRole().name(), usuario.getVersaoSessao());
        return new TokenResponse(token, usuario.getRole().name(), usuario.getNome());
    }

    private void registrarTentativaFalha(String email, String origemRequisicao) {
        limitePorEmail.registrarFalha(email);
        limitePorOrigem.registrarFalha(origemRequisicao);
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

        String token = jwtService.gerarToken(usuario.getEmail(), usuario.getRole().name(), usuario.getVersaoSessao());
        return new TokenResponse(token, usuario.getRole().name(), usuario.getNome());
    }

    public void alterarSenha(String emailUsuario, String senhaAtual, String novaSenha) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new UnauthorizedException("Senha atual incorreta");
        }

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        // Revoga qualquer token emitido antes desta troca (Task 9): o filtro compara essa
        // versão com o claim "v" do JWT a cada requisição.
        usuario.setVersaoSessao(usuario.getVersaoSessao() + 1);
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

}
