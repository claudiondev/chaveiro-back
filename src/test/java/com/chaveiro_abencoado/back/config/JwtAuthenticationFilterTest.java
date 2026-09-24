package com.chaveiro_abencoado.back.config;

import com.chaveiro_abencoado.back.model.UserRole;
import com.chaveiro_abencoado.back.model.Usuario;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import com.chaveiro_abencoado.back.service.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private FilterChain filterChain;

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("chave-secreta-teste-apenas-32-caracteres-minimo!!", 86400000);
        filter = new JwtAuthenticationFilter(jwtService, usuarioRepository);
        SecurityContextHolder.clearContext();
    }

    @Test
    void autenticaQuandoVersaoDeSessaoBateComAAtualDoUsuario() throws Exception {
        Usuario usuario = new Usuario("Teste", "teste@email.com", "hash", UserRole.DONO);
        usuario.setVersaoSessao(3);
        when(usuarioRepository.findByEmail("teste@email.com")).thenReturn(Optional.of(usuario));

        String token = jwtService.gerarToken("teste@email.com", "DONO", 3);
        var request = requisicaoComToken(token);
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void naoAutenticaQuandoSenhaFoiTrocadaDepoisDoTokenSerEmitido() throws Exception {
        Usuario usuario = new Usuario("Teste", "teste@email.com", "hash", UserRole.DONO);
        usuario.setVersaoSessao(2); // senha trocada depois que o token foi emitido com v=1
        when(usuarioRepository.findByEmail("teste@email.com")).thenReturn(Optional.of(usuario));

        String tokenAntigo = jwtService.gerarToken("teste@email.com", "DONO", 1);
        var request = requisicaoComToken(tokenAntigo);
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "token com versao de sessao antiga nao deveria autenticar");
    }

    @Test
    void naoAutenticaUsuarioDesativado() throws Exception {
        Usuario usuario = new Usuario("Teste", "teste@email.com", "hash", UserRole.DONO);
        usuario.setAtivo(false);
        when(usuarioRepository.findByEmail("teste@email.com")).thenReturn(Optional.of(usuario));

        String token = jwtService.gerarToken("teste@email.com", "DONO", 1);
        var request = requisicaoComToken(token);
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private MockHttpServletRequest requisicaoComToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
