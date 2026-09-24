package com.chaveiro_abencoado.back;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

// Utilitário compartilhado pelos testes @Tag("postgres-it"): cria/derruba um banco isolado
// direto no PostgreSQL local usado em dev, conectando primeiro ao banco "postgres".
final class PostgresItSupport {
    private PostgresItSupport() {}

    static void recriarBanco(String nomeBanco) throws SQLException {
        String host = System.getenv().getOrDefault("DB_IT_HOST", "localhost");
        String port = System.getenv().getOrDefault("DB_IT_PORT", "5432");
        String usuario = System.getenv().getOrDefault("DB_IT_USERNAME", "postgres");
        String senha = System.getenv().getOrDefault("DB_IT_PASSWORD", "postgres");
        String url = "jdbc:postgresql://" + host + ":" + port + "/postgres";

        try (Connection conexao = DriverManager.getConnection(url, usuario, senha);
             Statement statement = conexao.createStatement()) {
            statement.execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity "
                    + "WHERE datname = '" + nomeBanco + "' AND pid <> pg_backend_pid()");
            statement.execute("DROP DATABASE IF EXISTS " + nomeBanco);
            statement.execute("CREATE DATABASE " + nomeBanco);
        }
    }

    static String urlPara(String nomeBanco) {
        String host = System.getenv().getOrDefault("DB_IT_HOST", "localhost");
        String port = System.getenv().getOrDefault("DB_IT_PORT", "5432");
        return "jdbc:postgresql://" + host + ":" + port + "/" + nomeBanco;
    }
}
