package com.chaveiro_abencoado.back.repository;

import com.chaveiro_abencoado.back.model.FechamentoDiario;
import com.chaveiro_abencoado.back.model.StatusFechamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FechamentoDiarioRepository extends JpaRepository<FechamentoDiario, Long> {

    boolean existsByData(LocalDate data);

    Optional<FechamentoDiario> findTopByDataOrderByIdDesc(LocalDate data);

    Optional<FechamentoDiario> findByDataAndStatus(LocalDate data, StatusFechamento status);

    List<FechamentoDiario> findByDataBetween(LocalDate inicio, LocalDate fim);

    Page<FechamentoDiario> findByStatusOrderByDataDescIdDesc(StatusFechamento status, Pageable pageable);

    // SELECT ... FOR UPDATE: usadas só nas operações que alteram o caixa (registrar
    // movimentação/serviço, cancelar, fechar), nunca nas de consulta. Cada transação
    // bloqueia no máximo uma linha de fechamento, então não há risco de deadlock entre
    // duas transações concorrentes disputando linhas diferentes.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FechamentoDiario f WHERE f.data = :data AND f.status = :status")
    Optional<FechamentoDiario> findByDataAndStatusParaAtualizar(@Param("data") LocalDate data,
                                                                @Param("status") StatusFechamento status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FechamentoDiario f WHERE f.id = :id")
    Optional<FechamentoDiario> findByIdParaAtualizar(@Param("id") Long id);
}
