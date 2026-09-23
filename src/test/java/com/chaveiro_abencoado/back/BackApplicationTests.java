package com.chaveiro_abencoado.back;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class BackApplicationTests {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void deveCriarSchemaCompleto() {
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
