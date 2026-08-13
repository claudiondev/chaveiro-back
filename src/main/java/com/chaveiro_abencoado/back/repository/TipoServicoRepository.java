package com.chaveiro_abencoado.back.repository;

import com.chaveiro_abencoado.back.model.TipoServico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoServicoRepository extends JpaRepository<TipoServico, Long> {

    List<TipoServico> findByAtivoTrue();
}
