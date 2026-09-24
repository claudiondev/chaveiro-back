package com.chaveiro_abencoado.back.config;

import com.chaveiro_abencoado.back.model.UserRole;
import com.chaveiro_abencoado.back.model.Usuario;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonoInicialInitializerTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private Environment environment;

    private DonoInicialInitializer criar() {
        return new DonoInicialInitializer(usuarioRepository, passwordEncoder, environment);
    }

    @Test
    void naoFazNadaQuandoJaExisteDonoAtivo() {
        when(usuarioRepository.existsByRoleAndAtivoTrue(UserRole.DONO)).thenReturn(true);

        criar().run(null);

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void naoCriaNadaQuandoVariaveisIncompletas() {
        when(usuarioRepository.existsByRoleAndAtivoTrue(UserRole.DONO)).thenReturn(false);
        when(environment.getProperty("DONO_INICIAL_NOME")).thenReturn("Dono");
        when(environment.getProperty("DONO_INICIAL_EMAIL")).thenReturn(null);
        when(environment.getProperty("DONO_INICIAL_SENHA")).thenReturn("Senha123!");

        criar().run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void naoCriaNadaQuandoSenhaNaoAtendeRequisitos() {
        when(usuarioRepository.existsByRoleAndAtivoTrue(UserRole.DONO)).thenReturn(false);
        when(environment.getProperty("DONO_INICIAL_NOME")).thenReturn("Dono");
        when(environment.getProperty("DONO_INICIAL_EMAIL")).thenReturn("dono@loja.com");
        when(environment.getProperty("DONO_INICIAL_SENHA")).thenReturn("fraca");

        criar().run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void criaNovoDonoQuandoEmailNaoExiste() {
        when(usuarioRepository.existsByRoleAndAtivoTrue(UserRole.DONO)).thenReturn(false);
        when(environment.getProperty("DONO_INICIAL_NOME")).thenReturn("Claudio");
        when(environment.getProperty("DONO_INICIAL_EMAIL")).thenReturn("dono@loja.com");
        when(environment.getProperty("DONO_INICIAL_SENHA")).thenReturn("Senha123!");
        when(usuarioRepository.findByEmail("dono@loja.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Senha123!")).thenReturn("hash-forte");
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        criar().run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario salvo = captor.getValue();
        assertEquals("Claudio", salvo.getNome());
        assertEquals("dono@loja.com", salvo.getEmail());
        assertEquals("hash-forte", salvo.getSenha());
        assertEquals(UserRole.DONO, salvo.getRole());
        assertTrue(salvo.isAtivo());
    }

    @Test
    void reativaContaDesativadaComMesmoEmailEmVezDeCriarDuplicata() {
        Usuario desativado = new Usuario("Nome Antigo", "dono@loja.com", "hash-velho", UserRole.DONO);
        desativado.setId(1L);
        desativado.setAtivo(false);

        when(usuarioRepository.existsByRoleAndAtivoTrue(UserRole.DONO)).thenReturn(false);
        when(environment.getProperty("DONO_INICIAL_NOME")).thenReturn("Claudio");
        when(environment.getProperty("DONO_INICIAL_EMAIL")).thenReturn("dono@loja.com");
        when(environment.getProperty("DONO_INICIAL_SENHA")).thenReturn("Senha123!");
        when(usuarioRepository.findByEmail("dono@loja.com")).thenReturn(Optional.of(desativado));
        when(passwordEncoder.encode("Senha123!")).thenReturn("hash-novo");
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        criar().run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario salvo = captor.getValue();
        assertEquals(1L, salvo.getId(), "deveria reativar a mesma linha, nao criar outra");
        assertEquals("hash-novo", salvo.getSenha());
        assertTrue(salvo.isAtivo());
    }

    @Test
    void naoSobrescreveContaComEmailJaUsadoPorOutroRole() {
        Usuario funcionario = new Usuario("Func", "dono@loja.com", "hash", UserRole.FUNCIONARIO);

        when(usuarioRepository.existsByRoleAndAtivoTrue(UserRole.DONO)).thenReturn(false);
        when(environment.getProperty("DONO_INICIAL_NOME")).thenReturn("Claudio");
        when(environment.getProperty("DONO_INICIAL_EMAIL")).thenReturn("dono@loja.com");
        when(environment.getProperty("DONO_INICIAL_SENHA")).thenReturn("Senha123!");
        when(usuarioRepository.findByEmail("dono@loja.com")).thenReturn(Optional.of(funcionario));

        criar().run(null);

        verify(usuarioRepository, never()).save(any());
    }
}
