package br.com.navalha.presentation.http;
import com.sun.net.httpserver.*;
import java.io.*;
import java.util.*;

public final class StaticController implements HttpHandler {
    private final Map<String, String> assets = new HashMap<>();
    public StaticController() {
        assets.put("/", "text/html; charset=utf-8");
        assets.put("/index.html", "text/html; charset=utf-8");
        assets.put("/style.css", "text/css; charset=utf-8");
        assets.put("/app.js", "application/javascript; charset=utf-8");
        assets.put("/api.js", "application/javascript; charset=utf-8");
    }
    @Override public void handle(HttpExchange e) throws IOException {
        try {
            String path = e.getRequestURI().getPath();
            if (!e.getRequestMethod().equals("GET") || !assets.containsKey(path)) { e.sendResponseHeaders(404, -1); return; }
            String file = path.equals("/") ? "/index.html" : path;
            try (InputStream in = getClass().getResourceAsStream("/web" + file)) {
                if (in == null) { e.sendResponseHeaders(404, -1); return; }
                e.getResponseHeaders().set("Content-Type", assets.get(path));
                e.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
                e.getResponseHeaders().set("Cache-Control", "no-cache");
                e.sendResponseHeaders(200, 0);
                byte[] buffer = new byte[4096]; int n;
                while ((n = in.read(buffer)) != -1) e.getResponseBody().write(buffer, 0, n);
            }
        } finally { e.close(); }
    }
}
