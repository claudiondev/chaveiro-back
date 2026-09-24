package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.ProgressoAjudaRequest;
import com.chaveiro_abencoado.back.dto.ProgressoAjudaResponse;
import com.chaveiro_abencoado.back.exception.BusinessException;
import com.chaveiro_abencoado.back.exception.NotFoundException;
import com.chaveiro_abencoado.back.model.GuiaAjuda;
import com.chaveiro_abencoado.back.model.ProgressoAjuda;
import com.chaveiro_abencoado.back.model.Usuario;
import com.chaveiro_abencoado.back.repository.ProgressoAjudaRepository;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AjudaService {

    private final ProgressoAjudaRepository progressoRepository;
    private final UsuarioRepository usuarioRepository;

    public AjudaService(ProgressoAjudaRepository progressoRepository,
                        UsuarioRepository usuarioRepository) {
        this.progressoRepository = progressoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<ProgressoAjudaResponse> listar(String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);
        return progressoRepository.findAllByUsuarioIdOrderByGuia(usuario.getId()).stream()
                .map(this::paraResponse)
                .toList();
    }

    @Transactional
    public ProgressoAjudaResponse salvar(GuiaAjuda guia,
                                         ProgressoAjudaRequest request,
                                         String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);
        Optional<ProgressoAjuda> progressoExistente = progressoRepository
                .findByUsuarioIdAndGuia(usuario.getId(), guia);
        ProgressoAjuda progresso = progressoExistente
                .orElseGet(() -> new ProgressoAjuda(usuario, guia));

        if (progressoExistente.isPresent() && request.getVersao() < progresso.getVersao()) {
            throw new BusinessException("Versão do guia desatualizada");
        }

        progresso.setVersao(request.getVersao());
        progresso.setStatus(request.getStatus());
        progresso.setAtualizadoEm(LocalDateTime.now());
        return paraResponse(progressoRepository.save(progresso));
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    private ProgressoAjudaResponse paraResponse(ProgressoAjuda progresso) {
        return new ProgressoAjudaResponse(
                progresso.getGuia(),
                progresso.getVersao(),
                progresso.getStatus(),
                progresso.getAtualizadoEm()
        );
    }
}
