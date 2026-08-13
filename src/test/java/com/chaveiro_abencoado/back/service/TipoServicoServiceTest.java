package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.TipoServicoDTO;
import com.chaveiro_abencoado.back.dto.TipoServicoRequest;
import com.chaveiro_abencoado.back.model.CategoriaServico;
import com.chaveiro_abencoado.back.model.TipoServico;
import com.chaveiro_abencoado.back.repository.TipoServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TipoServicoServiceTest {

    @Mock
    private TipoServicoRepository repository;

    private TipoServicoService service;

    @BeforeEach
    void setUp() {
        service = new TipoServicoService(repository);
    }

    @Test
    void deveListarApenasAtivos() {
        TipoServico tipo = new TipoServico("Chave simples", new BigDecimal("15.00"),
                CategoriaServico.CHAVE, true);
        tipo.setId(1L);

        when(repository.findByAtivoTrue()).thenReturn(List.of(tipo));

        List<TipoServicoDTO> result = service.listarAtivos();

        assertEquals(1, result.size());
        assertEquals("Chave simples", result.get(0).getNome());
    }

    @Test
    void deveCriarTipoServico() {
        TipoServicoRequest request = new TipoServicoRequest();
        request.setNome("Chave tetra");
        request.setPreco(new BigDecimal("25.00"));
        request.setCategoria(CategoriaServico.CHAVE);
        request.setEhChave(true);

        when(repository.save(any(TipoServico.class))).thenAnswer(invocation -> {
            TipoServico t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        TipoServicoDTO dto = service.criar(request);

        assertEquals("Chave tetra", dto.getNome());
        assertTrue(dto.isEhChave());
        verify(repository).save(any(TipoServico.class));
    }

    @Test
    void deveDesativarTipoServico() {
        TipoServico tipo = new TipoServico("Teste", new BigDecimal("10.00"),
                CategoriaServico.OUTROS, false);
        tipo.setId(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(tipo));
        when(repository.save(any(TipoServico.class))).thenReturn(tipo);

        service.desativar(1L);

        assertFalse(tipo.isAtivo());
        verify(repository).save(tipo);
    }

    @Test
    void deveLancarExcecaoQuandoNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.buscarPorId(99L));
    }
}
