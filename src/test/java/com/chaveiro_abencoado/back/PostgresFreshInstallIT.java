package com.chaveiro_abencoado.back;

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

// Instalação do zero: banco vazio + todas as migrations do Flyway + ddl-auto=validate.
// Exige um PostgreSQL local acessível (mesmo usado em dev); não roda no `mvn test`, só no
// `mvn verify` ou `mvn failsafe:integration-test`. Veja CLAUDE.md para como rodar localmente.
@Tag("postgres-it")
@SpringBootTest(properties = "spring.datasource.url=jdbc:postgresql://${DB_IT_HOST:localhost}:${DB_IT_PORT:5432}/chaveiro_it_fresh")
@ActiveProfiles("postgres-it")
class PostgresFreshInstallIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void recriarBancoVazio() throws SQLException {
        PostgresItSupport.recriarBanco("chaveiro_it_fresh");
    }

    @Test
    void migrationsCriamSchemaCompativelComAsEntidades() {
        // Se o Hibernate validasse com sucesso, o contexto já teria subido; aqui só confirmamos
        // que o Flyway rodou as 4 migrations e nenhuma falhou.
        Integer totalAplicadas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        Integer totalFalhas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = false", Integer.class);

        assertEquals(4, totalAplicadas, "esperava V1, V2, V3 e V4 aplicadas com sucesso");
        assertEquals(0, totalFalhas);
    }

    @Test
    void seedDeTiposDeServicoFoiAplicado() {
        Integer tipos = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tipos_servico", Integer.class);
        assertTrue(tipos > 0, "V2 deveria semear ao menos um tipo de serviço");
    }
}
