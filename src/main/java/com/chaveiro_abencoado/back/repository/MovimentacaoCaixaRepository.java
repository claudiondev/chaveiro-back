package com.chaveiro_abencoado.back.repository;

import com.chaveiro_abencoado.back.model.MovimentacaoCaixa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MovimentacaoCaixaRepository extends JpaRepository<MovimentacaoCaixa, Long> {

    List<MovimentacaoCaixa> findByFechamentoDiarioId(Long fechamentoDiarioId);

    List<MovimentacaoCaixa> findByFechamentoDiarioIdIn(Collection<Long> fechamentoIds);

    void deleteByDescricaoAndFechamentoDiarioId(String descricao, Long fechamentoDiarioId);

    void deleteByDescricaoStartingWithAndFechamentoDiarioId(String descricao, Long fechamentoDiarioId);
}
