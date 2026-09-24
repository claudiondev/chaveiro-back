package com.chaveiro_abencoado.back;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Sobe o contexto e deixa o Hibernate criar o schema no H2 (ddl-auto=create-drop, Flyway
// desabilitado no profile "test"). Isso confirma que as entidades JPA são consistentes entre si,
// mas NÃO valida as migrations do Flyway nem o schema real do PostgreSQL — isso é feito pelos
// testes @Tag("postgres-it") em PostgresMigrationIT, que rodam contra um PostgreSQL de verdade.
@SpringBootTest
@ActiveProfiles("test")
class HibernateSchemaH2Test {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void hibernateDeveCriarEntidadesConsistentesNoH2() {
		Integer tabelaFechamentos = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM INFORMATION_SCHEMA.TABLES
				WHERE UPPER(TABLE_NAME) = 'FECHAMENTOS_DIARIOS'
				""", Integer.class);

		Integer colunaFormaPagamento = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM INFORMATION_SCHEMA.COLUMNS
				WHERE UPPER(TABLE_NAME) = 'MOVIMENTACOES_CAIXA'
				  AND UPPER(COLUMN_NAME) = 'FORMA_PAGAMENTO'
				""", Integer.class);

		assertEquals(1, tabelaFechamentos);
		assertEquals(1, colunaFormaPagamento);
	}

}
