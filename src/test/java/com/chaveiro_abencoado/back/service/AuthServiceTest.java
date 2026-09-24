package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.CadastroRequest;
import com.chaveiro_abencoado.back.dto.LoginRequest;
import com.chaveiro_abencoado.back.dto.TokenResponse;
import com.chaveiro_abencoado.back.model.UserRole;
import com.chaveiro_abencoado.back.model.Usuario;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private AuthService authService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        JwtService jwtService = new JwtService(
                "chave-secreta-teste-apenas-32-caracteres-minimo!!", 86400000);
        authService = new AuthService(usuarioRepository, jwtService, passwordEncoder);
    }

    @Test
    void deveLogarComSucesso() {
        Usuario usuario = new Usuario("Teste", "teste@email.com",
                passwordEncoder.encode("Senha123"), UserRole.DONO);
        usuario.setId(1L);

        when(usuarioRepository.findByEmail("teste@email.com")).thenReturn(Optional.of(usuario));

        LoginRequest request = new LoginRequest();
        request.setEmail("teste@email.com");
        request.setSenha("Senha123");

        TokenResponse response = authService.login(request, "127.0.0.1");

        assertNotNull(response.getToken());
        assertEquals("DONO", response.getRole());
        assertEquals("Teste", response.getNome());
    }

    @Test
    void deveRejeitarSenhaErrada() {
        Usuario usuario = new Usuario("Teste", "teste@email.com",
                passwordEncoder.encode("Senha123"), UserRole.DONO);

        when(usuarioRepository.findByEmail("teste@email.com")).thenReturn(Optional.of(usuario));

        LoginRequest request = new LoginRequest();
        request.setEmail("teste@email.com");
        request.setSenha("SenhaErrada1");

        assertThrows(RuntimeException.class, () -> authService.login(request, "127.0.0.1"));
    }

    @Test
    void deveRejeitarUsuarioInativo() {
        Usuario usuario = new Usuario("Teste", "teste@email.com",
                passwordEncoder.encode("Senha123"), UserRole.FUNCIONARIO);
        usuario.setAtivo(false);

        when(usuarioRepository.findByEmail("teste@email.com")).thenReturn(Optional.of(usuario));

        LoginRequest request = new LoginRequest();
        request.setEmail("teste@email.com");
        request.setSenha("Senha123");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request, "127.0.0.1"));
        assertEquals("Usuário desativado", ex.getMessage());
    }

    @Test
    void deveBloquearAposCincoTentativasComOMesmoEmail() {
        when(usuarioRepository.findByEmail("alvo@email.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("alvo@email.com");
        request.setSenha("qualquer");

        for (int i = 0; i < 5; i++) {
            String origem = "10.0.0." + i;
            assertThrows(RuntimeException.class, () -> authService.login(request, origem));
        }

        com.chaveiro_abencoado.back.exception.RateLimitException ex = assertThrows(
                com.chaveiro_abencoado.back.exception.RateLimitException.class,
                () -> authService.login(request, "10.0.0.99"));
        assertTrue(ex.getMessage().contains("Muitas tentativas"));
    }

    @Test
    void deveBloquearPorOrigemMesmoComEmailsDiferentes() {
        LoginRequest request = new LoginRequest();
        request.setSenha("qualquer");

        for (int i = 0; i < 5; i++) {
            String email = "email" + i + "@email.com";
            when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());
            request.setEmail(email);
            String origem = "203.0.113.5";
            assertThrows(RuntimeException.class, () -> authService.login(request, origem));
        }

        // Bloqueado pela origem antes mesmo de consultar o repositório por este e-mail novo
        request.setEmail("email-novo-nunca-usado@email.com");
        assertThrows(com.chaveiro_abencoado.back.exception.RateLimitException.class,
                () -> authService.login(request, "203.0.113.5"));
    }

    @Test
    void loginComSucessoLimpaTentativasAnteriores() {
        Usuario usuario = new Usuario("Teste", "recupera@email.com",
                passwordEncoder.encode("Senha123"), UserRole.DONO);
        when(usuarioRepository.findByEmail("recupera@email.com")).thenReturn(Optional.of(usuario));

        LoginRequest request = new LoginRequest();
        request.setEmail("recupera@email.com");
        request.setSenha("SenhaErrada");

        for (int i = 0; i < 4; i++) {
            String origem = "10.1.1." + i;
            assertThrows(RuntimeException.class, () -> authService.login(request, origem));
        }

        request.setSenha("Senha123");
        TokenResponse response = authService.login(request, "10.1.1.99");
        assertNotNull(response.getToken());

        // Depois do sucesso, o contador do e-mail volta a zero: mais 4 falhas não bloqueiam
        request.setSenha("SenhaErrada");
        for (int i = 0; i < 4; i++) {
            String origem = "10.2.2." + i;
            assertThrows(RuntimeException.class, () -> authService.login(request, origem));
        }
        request.setSenha("Senha123");
        TokenResponse segundaVez = authService.login(request, "10.2.2.99");
        assertNotNull(segundaVez.getToken());
    }

    @Test
    void deveCadastrarFuncionario() {
        when(usuarioRepository.existsByEmail("novo@email.com")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });

        CadastroRequest request = new CadastroRequest();
        request.setNome("Novo Funcionário");
        request.setEmail("novo@email.com");
        request.setSenha("Senha123");

        TokenResponse response = authService.cadastrar(request);

        assertNotNull(response.getToken());
        assertEquals("FUNCIONARIO", response.getRole());
    }

    @Test
    void deveRejeitarEmailDuplicado() {
        when(usuarioRepository.existsByEmail("existe@email.com")).thenReturn(true);

        CadastroRequest request = new CadastroRequest();
        request.setNome("Teste");
        request.setEmail("existe@email.com");
        request.setSenha("Senha123");

        assertThrows(RuntimeException.class, () -> authService.cadastrar(request));
    }
}
