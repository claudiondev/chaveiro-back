package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.ProgressoAjudaRequest;
import com.chaveiro_abencoado.back.dto.ProgressoAjudaResponse;
import com.chaveiro_abencoado.back.exception.BusinessException;
import com.chaveiro_abencoado.back.model.GuiaAjuda;
import com.chaveiro_abencoado.back.model.ProgressoAjuda;
import com.chaveiro_abencoado.back.model.StatusProgressoAjuda;
import com.chaveiro_abencoado.back.model.UserRole;
import com.chaveiro_abencoado.back.model.Usuario;
import com.chaveiro_abencoado.back.repository.ProgressoAjudaRepository;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AjudaServiceTest {

    @Mock
    private ProgressoAjudaRepository progressoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private AjudaService ajudaService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        ajudaService = new AjudaService(progressoRepository, usuarioRepository);
        usuario = new Usuario("Funcionário", "func@email.com", "senha", UserRole.FUNCIONARIO);
        usuario.setId(7L);
        when(usuarioRepository.findByEmail("func@email.com")).thenReturn(Optional.of(usuario));
    }

    @Test
    void deveCriarProgressoParaUsuarioAutenticado() {
        ProgressoAjudaRequest request = request(1, StatusProgressoAjuda.CONCLUIDO);
        when(progressoRepository.findByUsuarioIdAndGuia(7L, GuiaAjuda.CAIXA))
                .thenReturn(Optional.empty());
        when(progressoRepository.save(any(ProgressoAjuda.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProgressoAjudaResponse response = ajudaService.salvar(
                GuiaAjuda.CAIXA, request, "func@email.com");

        assertEquals(GuiaAjuda.CAIXA, response.guia());
        assertEquals(1, response.versao());
        assertEquals(StatusProgressoAjuda.CONCLUIDO, response.status());
    }

    @Test
    void deveAtualizarProgressoExistente() {
        ProgressoAjuda progresso = new ProgressoAjuda(usuario, GuiaAjuda.INICIO);
        progresso.setVersao(1);
        progresso.setStatus(StatusProgressoAjuda.DISPENSADO);
        when(progressoRepository.findByUsuarioIdAndGuia(7L, GuiaAjuda.INICIO))
                .thenReturn(Optional.of(progresso));
        when(progressoRepository.save(progresso)).thenReturn(progresso);

        ProgressoAjudaResponse response = ajudaService.salvar(
                GuiaAjuda.INICIO, request(2, StatusProgressoAjuda.CONCLUIDO), "func@email.com");

        assertEquals(2, response.versao());
        assertEquals(StatusProgressoAjuda.CONCLUIDO, response.status());
    }

    @Test
    void deveRejeitarVersaoMaisAntiga() {
        ProgressoAjuda progresso = new ProgressoAjuda(usuario, GuiaAjuda.MENU);
        progresso.setVersao(2);
        progresso.setStatus(StatusProgressoAjuda.CONCLUIDO);
        when(progressoRepository.findByUsuarioIdAndGuia(7L, GuiaAjuda.MENU))
                .thenReturn(Optional.of(progresso));

        assertThrows(BusinessException.class, () -> ajudaService.salvar(
                GuiaAjuda.MENU,
                request(1, StatusProgressoAjuda.DISPENSADO),
                "func@email.com"
        ));
    }

    private ProgressoAjudaRequest request(int versao, StatusProgressoAjuda status) {
        ProgressoAjudaRequest request = new ProgressoAjudaRequest();
        request.setVersao(versao);
        request.setStatus(status);
        return request;
    }
}
