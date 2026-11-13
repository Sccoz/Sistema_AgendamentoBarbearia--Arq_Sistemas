package br.com.navalha.infrastructure.config;

import org.h2.tools.Server;
import java.sql.SQLException;

/** Console web do H2 (uso acadêmico local). Detalhe de infraestrutura, fora do Main. */
public final class H2Console {
    private final Server server;

    private H2Console(Server server) { this.server = server; }

    public static H2Console iniciar(int porta) throws SQLException {
        // Sem -webAllowOthers: só aceita conexões locais.
        // -properties null evita escrever configurações na pasta pessoal.
        Server server = Server.createWebServer("-webPort", String.valueOf(porta),
                "-webDaemon", "-properties", "null").start();
        return new H2Console(server);
    }

    public void parar() { server.stop(); }
}
