package com.chaveiro_abencoado.back;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Evolução de um banco existente: aplica manualmente as migrations até V3 (simulando uma
// instalação já em produção antes da V4), depois deixa o Flyway do próprio Spring Boot
// completar para a versão mais nova. Confirma que quem já tem dados não quebra ao migrar
// e que o schema final ainda bate com as entidades (ddl-auto=validate).
@Tag("postgres-it")
@SpringBootTest(properties = "spring.datasource.url=jdbc:postgresql://${DB_IT_HOST:localhost}:${DB_IT_PORT:5432}/chaveiro_it_evolve")
@ActiveProfiles("postgres-it")
class PostgresMigrationEvolutionIT {

    private static final String BANCO = "chaveiro_it_evolve";
    private static final int VERSAO_ANTIGA = 3;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private Flyway flyway;

    @BeforeAll
    static void prepararBancoComVersaoAntiga() throws SQLException {
        PostgresItSupport.recriarBanco(BANCO);

        Flyway flywayAntigo = Flyway.configure()
                .dataSource(PostgresItSupport.urlPara(BANCO), "postgres", "postgres")
                .target(String.valueOf(VERSAO_ANTIGA))
                .load();
        MigrateResult resultado = flywayAntigo.migrate();

        assertEquals(VERSAO_ANTIGA, resultado.migrationsExecuted,
                "pré-condição: banco deveria parar em V" + VERSAO_ANTIGA);
    }

    @Test
    void bancoExistenteEvoluiParaAUltimaVersaoSemQuebrar() {
        // O @SpringBootTest já rodou o Flyway do app inteiro ao subir o contexto: como o banco
        // parou em V3, o restante (V4 em diante) deveria ter sido aplicado agora. Se o schema
        // resultante não batesse com as entidades, ddl-auto=validate teria impedido o contexto
        // de subir. Contagem não é hardcoded: cresce conforme novas migrations são adicionadas.
        int totalNoClasspath = flyway.info().all().length;
        Integer totalAplicadas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        Integer versaoMaisRecente = jdbcTemplate.queryForObject(
                "SELECT MAX(version) FROM flyway_schema_history WHERE success = true", Integer.class);

        assertEquals(totalNoClasspath, totalAplicadas,
                "V1..V" + VERSAO_ANTIGA + " (manuais) + o restante (pelo Spring) deveriam somar todas");
        assertEquals(totalNoClasspath, versaoMaisRecente);
    }

    @Test
    void dadosSemeadosAntesDaEvolucaoContinuamIntactos() {
        Integer tipos = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tipos_servico", Integer.class);
        assertTrue(tipos > 0, "seed da V2 deveria continuar presente após a evolução completa");
    }
}
