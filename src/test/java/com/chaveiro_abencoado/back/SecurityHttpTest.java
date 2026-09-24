package com.chaveiro_abencoado.back;

import com.chaveiro_abencoado.back.model.UserRole;
import com.chaveiro_abencoado.back.model.Usuario;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import com.chaveiro_abencoado.back.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityHttpTest {

    private static final String SENHA = "Senha123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Usuario dono;
    private Usuario funcionario;

    @BeforeEach
    void prepararUsuarios() {
        dono = salvarUsuario("Dono", "dono-http@teste.com", UserRole.DONO);
        funcionario = salvarUsuario("Funcionário", "funcionario-http@teste.com", UserRole.FUNCIONARIO);
    }

    @Test
    void permiteLoginSemToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"funcionario-http@teste.com","senha":"Senha123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("FUNCIONARIO"));
    }

    @Test
    void rejeitaRotaProtegidaSemTokenCom401EmUtf8() throws Exception {
        mockMvc.perform(get("/api/auth/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.erro").value("Não autenticado"));
    }

    @Test
    void bloqueiaFuncionarioEmRotaDoDonoCom403() throws Exception {
        mockMvc.perform(get("/api/auth/usuarios")
                        .header("Authorization", bearer(funcionario)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("Sem permissão para este recurso"));
    }

    @Test
    void permiteDonoEmRotaAdministrativa() throws Exception {
        mockMvc.perform(get("/api/auth/usuarios")
                        .header("Authorization", bearer(dono)))
                .andExpect(status().isOk());
    }

    @Test
    void rejeitaTokenRevogadoAposMudancaDaVersaoDaSessao() throws Exception {
        String tokenAntigo = bearer(dono);
        dono.setVersaoSessao(dono.getVersaoSessao() + 1);
        usuarioRepository.saveAndFlush(dono);

        mockMvc.perform(get("/api/auth/usuarios")
                        .header("Authorization", tokenAntigo))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejeitaTokenDeUsuarioDesativado() throws Exception {
        String token = bearer(funcionario);
        funcionario.setAtivo(false);
        usuarioRepository.saveAndFlush(funcionario);

        mockMvc.perform(get("/api/tipos-servico")
                        .header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bloqueiaCadastroDeFuncionarioPorOutroFuncionario() throws Exception {
        mockMvc.perform(post("/api/auth/cadastro")
                        .header("Authorization", bearer(funcionario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Novo","email":"novo@teste.com","senha":"Senha123"}
                                """))
                .andExpect(status().isForbidden());
    }

    private Usuario salvarUsuario(String nome, String email, UserRole role) {
        Usuario usuario = new Usuario(nome, email, passwordEncoder.encode(SENHA), role);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private String bearer(Usuario usuario) {
        String token = jwtService.gerarToken(
                usuario.getEmail(),
                usuario.getRole().name(),
                usuario.getVersaoSessao()
        );
        return "Bearer " + token;
    }
}
