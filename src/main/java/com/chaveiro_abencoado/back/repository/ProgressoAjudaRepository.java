package com.chaveiro_abencoado.back.repository;

import com.chaveiro_abencoado.back.model.GuiaAjuda;
import com.chaveiro_abencoado.back.model.ProgressoAjuda;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProgressoAjudaRepository extends JpaRepository<ProgressoAjuda, Long> {

    List<ProgressoAjuda> findAllByUsuarioIdOrderByGuia(Long usuarioId);

    Optional<ProgressoAjuda> findByUsuarioIdAndGuia(Long usuarioId, GuiaAjuda guia);

    // SELECT ... FOR UPDATE: serializa duas atualizações concorrentes pro mesmo par
    // usuário+guia, quando a linha já existe. Não ajuda na primeira gravação (a linha
    // ainda não existe pra travar) — esse caso é pego pela constraint única do banco e
    // tratado com retry em AjudaService.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProgressoAjuda p WHERE p.usuario.id = :usuarioId AND p.guia = :guia")
    Optional<ProgressoAjuda> findByUsuarioIdAndGuiaParaAtualizar(@Param("usuarioId") Long usuarioId,
                                                                 @Param("guia") GuiaAjuda guia);
}
