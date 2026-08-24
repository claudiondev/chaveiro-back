package com.chaveiro_abencoado.back.repository;

import com.chaveiro_abencoado.back.model.ServicoRealizado;
import com.chaveiro_abencoado.back.model.StatusPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ServicoRealizadoRepository extends JpaRepository<ServicoRealizado, Long> {

    List<ServicoRealizado> findByFechamentoDiarioId(Long fechamentoDiarioId);

    List<ServicoRealizado> findByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);

    List<ServicoRealizado> findByStatusPagamento(StatusPagamento statusPagamento);

    @Query("SELECT COALESCE(SUM(s.quantidade), 0) FROM ServicoRealizado s " +
           "WHERE s.tipoServico.ehChave = true AND s.dataHora BETWEEN :inicio AND :fim")
    Integer contarChavesNoPeriodo(@Param("inicio") LocalDateTime inicio,
                                  @Param("fim") LocalDateTime fim);
}
