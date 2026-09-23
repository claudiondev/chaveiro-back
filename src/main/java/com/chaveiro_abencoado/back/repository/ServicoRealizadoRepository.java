package com.chaveiro_abencoado.back.repository;

import com.chaveiro_abencoado.back.model.ServicoRealizado;
import com.chaveiro_abencoado.back.model.StatusPagamento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ServicoRealizadoRepository extends JpaRepository<ServicoRealizado, Long> {

    @EntityGraph(attributePaths = {"tipoServico"})
    List<ServicoRealizado> findByFechamentoDiarioId(Long fechamentoDiarioId);

    List<ServicoRealizado> findByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);

    List<ServicoRealizado> findByStatusPagamento(StatusPagamento statusPagamento);

    // Carrega tipo e usuário junto para evitar uma consulta por serviço nos relatórios
    @EntityGraph(attributePaths = {"tipoServico", "usuario"})
    List<ServicoRealizado> findByFechamentoDiarioIdIn(Collection<Long> fechamentoIds);
}
