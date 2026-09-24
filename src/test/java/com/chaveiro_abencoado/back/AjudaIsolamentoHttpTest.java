package com.chaveiro_abencoado.back;

import com.chaveiro_abencoado.back.model.UserRole;
import com.chaveiro_abencoado.back.model.Usuario;
import com.chaveiro_abencoado.back.repository.ProgressoAjudaRepository;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import com.chaveiro_abencoado.back.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sem @Transactional de propósito: AjudaService.salvar() usa PROPAGATION_REQUIRES_NEW
// (necessário pro retry de colisão na primeira gravação — ver AjudaService), que abre uma
// transação de banco separada. Com @Transactional + rollback automático no nível da classe
// de teste (como em SecurityHttpTest), essa transação nova não enxergaria os usuários
// criados no @BeforeEach (ainda não commitados) e o teste falharia com violação de FK —
// não é um bug de produção, é incompatibilidade entre REQUIRES_NEW e rollback de teste.
// Por isso este teste limpa os próprios dados manualmente no @AfterEach.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AjudaIsolamentoHttpTest {

    private static final String SENHA = "Senha123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ProgressoAjudaRepository progressoRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private Usuario dono;
    private Usuario funcionario;

    @BeforeEach
    void prepararUsuarios() {
        dono = usuarioRepository.save(new Usuario("Dono", "dono-isolamento@teste.com",
                passwordEncoder.encode(SENHA), UserRole.DONO));
        funcionario = usuarioRepository.save(new Usuario("Funcionário", "funcionario-isolamento@teste.com",
                passwordEncoder.encode(SENHA), UserRole.FUNCIONARIO));
    }

    @AfterEach
    void limparDados() {
        progressoRepository.findAllByUsuarioIdOrderByGuia(dono.getId()).forEach(progressoRepository::delete);
        progressoRepository.findAllByUsuarioIdOrderByGuia(funcionario.getId()).forEach(progressoRepository::delete);
        usuarioRepository.delete(dono);
        usuarioRepository.delete(funcionario);
    }

    @Test
    void progressoDeAjudaEIsoladoPorUsuario() throws Exception {
        // Dono conclui o guia CAIXA
        mockMvc.perform(put("/api/ajuda/progressos/CAIXA")
                        .header("Authorization", bearer(dono))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"versao":1,"status":"CONCLUIDO"}
                                """))
                .andExpect(status().isOk());

        // O funcionário, que nunca interagiu com nenhum guia, não vê o progresso do dono
        mockMvc.perform(get("/api/ajuda/progressos")
                        .header("Authorization", bearer(funcionario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        // O dono continua vendo o próprio progresso
        mockMvc.perform(get("/api/ajuda/progressos")
                        .header("Authorization", bearer(dono)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].guia").value("CAIXA"));

        // O funcionário dispensa o mesmo guia com sua própria conta — não colide com o dono
        mockMvc.perform(put("/api/ajuda/progressos/CAIXA")
                        .header("Authorization", bearer(funcionario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"versao":1,"status":"DISPENSADO"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/ajuda/progressos")
                        .header("Authorization", bearer(dono)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("CONCLUIDO"));
        mockMvc.perform(get("/api/ajuda/progressos")
                        .header("Authorization", bearer(funcionario)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("DISPENSADO"));
    }

    private String bearer(Usuario usuario) {
        String token = jwtService.gerarToken(usuario.getEmail(), usuario.getRole().name(), usuario.getVersaoSessao());
        return "Bearer " + token;
    }
}
