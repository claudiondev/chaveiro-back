package com.chaveiro_abencoado.back.repository;

import com.chaveiro_abencoado.back.model.ServicoRealizado;
import com.chaveiro_abencoado.back.model.StatusPagamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServicoRealizadoRepository extends JpaRepository<ServicoRealizado, Long> {

    @EntityGraph(attributePaths = {"tipoServico"})
    List<ServicoRealizado> findByFechamentoDiarioId(Long fechamentoDiarioId);

    List<ServicoRealizado> findByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);

    List<ServicoRealizado> findByStatusPagamento(StatusPagamento statusPagamento);

    // Carrega tipo e usuário junto para evitar uma consulta por serviço nos relatórios
    @EntityGraph(attributePaths = {"tipoServico", "usuario"})
    List<ServicoRealizado> findByFechamentoDiarioIdIn(Collection<Long> fechamentoIds);

    // SELECT ... FOR UPDATE: duas chamadas concorrentes de marcarComoPago() para o mesmo
    // serviço serializam aqui, então só a primeira encontra PENDENTE — a segunda vê PAGO
    // já commitado e recusa de forma limpa, em vez de gerar duas entradas.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ServicoRealizado s WHERE s.id = :id")
    Optional<ServicoRealizado> findByIdParaAtualizar(@Param("id") Long id);
}
