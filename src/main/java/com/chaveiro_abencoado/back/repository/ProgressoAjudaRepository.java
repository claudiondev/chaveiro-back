package com.chaveiro_abencoado.back.repository;

import com.chaveiro_abencoado.back.model.GuiaAjuda;
import com.chaveiro_abencoado.back.model.ProgressoAjuda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProgressoAjudaRepository extends JpaRepository<ProgressoAjuda, Long> {

    List<ProgressoAjuda> findAllByUsuarioIdOrderByGuia(Long usuarioId);

    Optional<ProgressoAjuda> findByUsuarioIdAndGuia(Long usuarioId, GuiaAjuda guia);
}
