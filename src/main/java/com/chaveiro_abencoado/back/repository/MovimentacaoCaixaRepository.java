package com.chaveiro_abencoado.back.repository;

import com.chaveiro_abencoado.back.model.MovimentacaoCaixa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimentacaoCaixaRepository extends JpaRepository<MovimentacaoCaixa, Long> {

    List<MovimentacaoCaixa> findByFechamentoDiarioId(Long fechamentoDiarioId);

    void deleteByDescricaoAndFechamentoDiarioId(String descricao, Long fechamentoDiarioId);
}
