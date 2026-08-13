package com.chaveiro_abencoado.back.repository;

import com.chaveiro_abencoado.back.model.FechamentoDiario;
import com.chaveiro_abencoado.back.model.StatusFechamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FechamentoDiarioRepository extends JpaRepository<FechamentoDiario, Long> {

    Optional<FechamentoDiario> findByData(LocalDate data);

    Optional<FechamentoDiario> findByDataAndStatus(LocalDate data, StatusFechamento status);

    List<FechamentoDiario> findByDataBetween(LocalDate inicio, LocalDate fim);
}
