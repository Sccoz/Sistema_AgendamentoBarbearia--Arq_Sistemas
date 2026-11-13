package br.com.navalha;

import br.com.navalha.application.port.AgendamentoRepository;
import br.com.navalha.application.service.AgendamentoService;
import br.com.navalha.infrastructure.config.DatabaseConfig;
import br.com.navalha.infrastructure.persistence.JdbcAgendamentoRepository;
import br.com.navalha.presentation.http.*;
import com.sun.net.httpserver.HttpServer;
import org.h2.tools.Server;
import java.net.InetSocketAddress;
import java.time.*;
import java.util.concurrent.*;

/** Composition root: conecta apresentação, aplicação e infraestrutura. */
public final class Main {
    public static void main(String[] args) throws Exception {
        AgendamentoRepository repository = new JdbcAgendamentoRepository(DatabaseConfig.criar());
        AgendamentoService service = new AgendamentoService(repository, Clock.system(ZoneId.of("America/Sao_Paulo")));
        int port = args.length == 0 ? 8080 : Integer.parseInt(args[0]);
        int consolePort = Integer.getInteger("barbearia.console.port", 8082);
        // O console aceita conexões locais; não habilitamos webAllowOthers.
        // -properties null evita escrever configurações na pasta pessoal.
        Server console = Server.createWebServer("-webPort", String.valueOf(consolePort), "-webDaemon", "-properties", "null").start();
        HttpServer server;
        try { server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0); }
        catch (Exception e) { console.stop(); throw e; }
        ExecutorService executor = Executors.newFixedThreadPool(8);
        server.setExecutor(executor);
        server.createContext("/api/", new AgendamentoController(service));
        server.createContext("/", new StaticController());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(1); executor.shutdown(); console.stop();
        }));
        server.start();
        System.out.println("Sistema: http://localhost:" + port);
        System.out.println("Console do banco H2: http://localhost:" + consolePort);
        System.out.println("JDBC URL: " + System.getProperty("barbearia.db.url", "jdbc:h2:file:./data/barbearia"));
        System.out.println("Usuario: sa | Senha: vazia, salvo se BARBEARIA_DB_PASSWORD estiver definida.");
    }
}
