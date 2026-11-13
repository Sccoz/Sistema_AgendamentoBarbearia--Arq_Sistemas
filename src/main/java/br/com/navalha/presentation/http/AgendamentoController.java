package br.com.navalha.presentation.http;

import br.com.navalha.application.dto.CriarAgendamentoCommand;
import br.com.navalha.application.exception.*;
import br.com.navalha.application.service.AgendamentoService;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/** Traduz HTTP em casos de uso. Nunca acessa o repositório. */
public final class AgendamentoController implements HttpHandler {
    private final AgendamentoService service;
    public AgendamentoController(AgendamentoService service) { this.service = service; }
    @Override public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            Map<String, String> query = decode(exchange.getRequestURI().getRawQuery());
            if (path.equals("/api/catalogo") && method.equals("GET")) respond(exchange, 200, JsonPresenter.catalogo());
            else if (path.equals("/api/horarios") && method.equals("GET")) respond(exchange, 200,
                service.horarios(query.get("data"), query.get("servico"), query.get("barbeiro")).stream()
                    .map(t -> JsonPresenter.quote(t.toString())).collect(Collectors.joining(",", "[", "]")));
            else if (path.equals("/api/agendamentos") && method.equals("GET")) respond(exchange, 200, JsonPresenter.lista(service.listar(query.get("data"))));
            else if (path.equals("/api/agendamentos") && method.equals("POST")) {
                String type = exchange.getRequestHeaders().getFirst("Content-Type");
                if (type == null || !type.split(";")[0].trim().equalsIgnoreCase("application/x-www-form-urlencoded")) {
                    respond(exchange, 415, "{\"mensagem\":\"Envie um formulário URL encoded.\"}"); return;
                }
                Map<String, String> body = decode(readBody(exchange.getRequestBody()));
                respond(exchange, 201, JsonPresenter.agendamento(service.criar(new CriarAgendamentoCommand(
                    body.get("cliente"), body.get("telefone"), body.get("servico"), body.get("barbeiro"), body.get("data"), body.get("horario")))));
            } else if (path.startsWith("/api/agendamentos/") && method.equals("DELETE")) {
                service.cancelar(path.substring("/api/agendamentos/".length()));
                exchange.sendResponseHeaders(204, -1);
            } else respond(exchange, 404, "{\"mensagem\":\"Rota ou método não disponível.\"}");
        } catch (RegraNegocioException | IllegalArgumentException e) {
            respond(exchange, 400, "{\"mensagem\":" + JsonPresenter.quote(e.getMessage()) + "}");
        } catch (NaoEncontradoException e) {
            respond(exchange, 404, "{\"mensagem\":" + JsonPresenter.quote(e.getMessage()) + "}");
        } catch (Exception e) {
            e.printStackTrace();
            respond(exchange, 500, "{\"mensagem\":\"Erro interno ao processar a solicitação.\"}");
        } finally { exchange.close(); }
    }
    private Map<String, String> decode(String raw) throws UnsupportedEncodingException {
        Map<String, String> result = new HashMap<>();
        if (raw != null && !raw.isEmpty()) for (String pair : raw.split("&")) {
            String[] parts = pair.split("=", 2);
            result.put(URLDecoder.decode(parts[0], "UTF-8"), parts.length == 2 ? URLDecoder.decode(parts[1], "UTF-8") : "");
        }
        return result;
    }
    private String readBody(InputStream input) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024]; int read;
        while ((read = input.read(buffer)) != -1) {
            if (bytes.size() + read > 8192) throw new IllegalArgumentException("Formulário excede o limite de 8 KB.");
            bytes.write(buffer, 0, read);
        }
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }
    private void respond(HttpExchange e, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        e.getResponseHeaders().set("Cache-Control", "no-store");
        e.sendResponseHeaders(status, bytes.length);
        e.getResponseBody().write(bytes);
    }
}
