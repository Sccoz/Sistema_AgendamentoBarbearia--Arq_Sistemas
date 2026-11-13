package br.com.navalha.infrastructure.config;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;
import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;

/** Configuração específica de H2. Não pertence ao domínio ou ao serviço. */
public final class DatabaseConfig {
    private DatabaseConfig() { }

    public static DataSource criar() throws IOException, SQLException {
        String url = System.getProperty("barbearia.db.url", "jdbc:h2:file:./data/barbearia");
        if (url.equals("jdbc:h2:file:./data/barbearia")) Files.createDirectories(Paths.get("data"));
        JdbcDataSource source = new JdbcDataSource();
        source.setURL(url);
        source.setUser("sa");
        String password = System.getenv("BARBEARIA_DB_PASSWORD");
        source.setPassword(password == null ? "" : password);
        try (InputStream input = DatabaseConfig.class.getResourceAsStream("/db/schema.sql")) {
            if (input == null) throw new IOException("Recurso db/schema.sql não encontrado.");
            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
                 Connection connection = source.getConnection()) {
                RunScript.execute(connection, reader);
            }
        }
        return source;
    }
}
