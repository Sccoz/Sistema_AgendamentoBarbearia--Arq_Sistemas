package br.com.navalha;

import br.com.navalha.application.port.AgendamentoRepository;
import br.com.navalha.application.service.AgendamentoService;
import br.com.navalha.infrastructure.persistence.InMemoryAgendamentoRepository;
import br.com.navalha.presentation.http.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.*;
import java.util.concurrent.*;

/** Composition root: único ponto que conhece e conecta todas as camadas. */
public final class Main {
    public static void main(String[] args) throws Exception {
        AgendamentoRepository repository = new InMemoryAgendamentoRepository();
        AgendamentoService service = new AgendamentoService(repository, Clock.system(ZoneId.of("America/Sao_Paulo")));
        int port = args.length == 0 ? 8080 : Integer.parseInt(args[0]);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        server.setExecutor(executor);
        server.createContext("/api/", new AgendamentoController(service));
        server.createContext("/", new StaticController());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { server.stop(1); executor.shutdown(); }));
        server.start();
        System.out.println("Navalha: http://localhost:" + port + " (dados em memoria)");
    }
}
