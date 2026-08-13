package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.AberturaRequest;
import com.chaveiro_abencoado.back.dto.FechamentoResponse;
import com.chaveiro_abencoado.back.model.FechamentoDiario;
import com.chaveiro_abencoado.back.model.StatusFechamento;
import com.chaveiro_abencoado.back.model.UserRole;
import com.chaveiro_abencoado.back.model.Usuario;
import com.chaveiro_abencoado.back.repository.FechamentoDiarioRepository;
import com.chaveiro_abencoado.back.repository.MovimentacaoCaixaRepository;
import com.chaveiro_abencoado.back.repository.ServicoRealizadoRepository;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaixaServiceTest {

    @Mock
    private FechamentoDiarioRepository fechamentoRepository;
    @Mock
    private MovimentacaoCaixaRepository movimentacaoRepository;
    @Mock
    private ServicoRealizadoRepository servicoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private CaixaService caixaService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        caixaService = new CaixaService(fechamentoRepository, movimentacaoRepository,
                servicoRepository, usuarioRepository);
        usuario = new Usuario("Dono", "dono@email.com", "hash", UserRole.DONO);
        usuario.setId(1L);
    }

    @Test
    void deveAbrirCaixa() {
        when(fechamentoRepository.findByDataAndStatus(LocalDate.now(), StatusFechamento.ABERTO))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail("dono@email.com")).thenReturn(Optional.of(usuario));
        when(fechamentoRepository.save(any(FechamentoDiario.class))).thenAnswer(invocation -> {
            FechamentoDiario f = invocation.getArgument(0);
            f.setId(1L);
            return f;
        });

        AberturaRequest request = new AberturaRequest();
        request.setValorAbertura(new BigDecimal("200.00"));

        FechamentoResponse response = caixaService.abrirCaixa(request, "dono@email.com");

        assertEquals(new BigDecimal("200.00"), response.getValorAbertura());
        assertEquals(StatusFechamento.ABERTO, response.getStatus());
    }

    @Test
    void deveRejeitarCaixaDuplicado() {
        FechamentoDiario existente = new FechamentoDiario(LocalDate.now(),
                new BigDecimal("100.00"), usuario);

        when(fechamentoRepository.findByDataAndStatus(LocalDate.now(), StatusFechamento.ABERTO))
                .thenReturn(Optional.of(existente));

        AberturaRequest request = new AberturaRequest();
        request.setValorAbertura(new BigDecimal("200.00"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> caixaService.abrirCaixa(request, "dono@email.com"));
        assertEquals("Já existe um caixa aberto para hoje", ex.getMessage());
    }

    @Test
    void deveFecharCaixa() {
        FechamentoDiario fechamento = new FechamentoDiario(LocalDate.now(),
                new BigDecimal("200.00"), usuario);
        fechamento.setId(1L);

        when(fechamentoRepository.findByDataAndStatus(LocalDate.now(), StatusFechamento.ABERTO))
                .thenReturn(Optional.of(fechamento));
        when(movimentacaoRepository.findByFechamentoDiarioId(1L)).thenReturn(Collections.emptyList());
        when(servicoRepository.findByDataHoraBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(servicoRepository.contarChavesNoPeriodo(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0);
        when(fechamentoRepository.save(any(FechamentoDiario.class))).thenReturn(fechamento);

        FechamentoResponse response = caixaService.fecharCaixa("Dia tranquilo");

        assertEquals(StatusFechamento.FECHADO, response.getStatus());
    }

    @Test
    void deveRejeitarFechamentoSemCaixaAberto() {
        when(fechamentoRepository.findByDataAndStatus(LocalDate.now(), StatusFechamento.ABERTO))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> caixaService.fecharCaixa(null));
    }
}
