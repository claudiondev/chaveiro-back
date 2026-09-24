package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.dto.ProgressoAjudaRequest;
import com.chaveiro_abencoado.back.dto.ProgressoAjudaResponse;
import com.chaveiro_abencoado.back.exception.NotFoundException;
import com.chaveiro_abencoado.back.model.GuiaAjuda;
import com.chaveiro_abencoado.back.model.ProgressoAjuda;
import com.chaveiro_abencoado.back.model.StatusProgressoAjuda;
import com.chaveiro_abencoado.back.model.Usuario;
import com.chaveiro_abencoado.back.repository.ProgressoAjudaRepository;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AjudaService {

    private final ProgressoAjudaRepository progressoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TransactionTemplate novaTransacao;

    public AjudaService(ProgressoAjudaRepository progressoRepository,
                        UsuarioRepository usuarioRepository,
                        PlatformTransactionManager transactionManager) {
        this.progressoRepository = progressoRepository;
        this.usuarioRepository = usuarioRepository;
        // REQUIRES_NEW: cada tentativa roda na sua própria transação. Necessário pro
        // retry funcionar — depois que o Postgres rejeita um INSERT por violar a
        // constraint única, a transação de banco fica "abortada" e não aceita mais
        // nenhum comando; só uma transação nova consegue reconsultar e tentar de novo.
        this.novaTransacao = new TransactionTemplate(transactionManager);
        this.novaTransacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public List<ProgressoAjudaResponse> listar(String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);
        return progressoRepository.findAllByUsuarioIdOrderByGuia(usuario.getId()).stream()
                .map(this::paraResponse)
                .toList();
    }

    // Duas primeiras gravações concorrentes pro mesmo par usuário+guia: nenhuma linha
    // existe ainda pra travar com SELECT ... FOR UPDATE, então as duas tentam INSERT; a
    // que perde esbarra em uk_progresso_ajuda_usuario_guia. Em vez de propagar como 500,
    // tentamos de novo numa transação nova — dessa vez a linha já existe, o lock
    // funciona, e a regra de precedência decide o resultado final.
    public ProgressoAjudaResponse salvar(GuiaAjuda guia, ProgressoAjudaRequest request, String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);
        try {
            return novaTransacao.execute(status -> salvarComPrecedencia(usuario, guia, request));
        } catch (DataIntegrityViolationException colisaoNaPrimeiraGravacao) {
            return novaTransacao.execute(status -> salvarComPrecedencia(usuario, guia, request));
        }
    }

    private ProgressoAjudaResponse salvarComPrecedencia(Usuario usuario, GuiaAjuda guia, ProgressoAjudaRequest request) {
        Optional<ProgressoAjuda> existente = progressoRepository
                .findByUsuarioIdAndGuiaParaAtualizar(usuario.getId(), guia);

        if (existente.isPresent() && naoDevePrevalecer(existente.get(), request)) {
            // Cliente atrasado (rede lenta, retry, ou já perdeu a corrida acima): devolve
            // o que já está valendo, sem erro — idempotente do ponto de vista de quem chama.
            return paraResponse(existente.get());
        }

        ProgressoAjuda progresso = existente.orElseGet(() -> new ProgressoAjuda(usuario, guia));
        progresso.setVersao(request.getVersao());
        progresso.setStatus(request.getStatus());
        progresso.setAtualizadoEm(LocalDateTime.now());
        return paraResponse(progressoRepository.save(progresso));
    }

    // Versão mais nova sempre prevalece; na mesma versão, uma dispensa não apaga uma
    // conclusão (dispensa "atrasada" chegando depois de o usuário já ter terminado o tour).
    private boolean naoDevePrevalecer(ProgressoAjuda atual, ProgressoAjudaRequest novo) {
        if (novo.getVersao() < atual.getVersao()) {
            return true;
        }
        return novo.getVersao() == atual.getVersao()
                && atual.getStatus() == StatusProgressoAjuda.CONCLUIDO
                && novo.getStatus() == StatusProgressoAjuda.DISPENSADO;
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
