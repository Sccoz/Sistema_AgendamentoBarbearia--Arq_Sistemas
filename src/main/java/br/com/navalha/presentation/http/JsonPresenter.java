package br.com.navalha.presentation.http;

import br.com.navalha.domain.*;
import java.util.List;
import java.util.stream.Collectors;

/** Serialização das respostas; sem regras de negócio. */
final class JsonPresenter {
    private JsonPresenter() { }
    static String quote(String texto) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : texto.toCharArray()) {
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c < 32) out.append(String.format("\\u%04x", (int) c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
    static String agendamento(Agendamento a) {
        return "{\"id\":" + quote(a.getId().toString()) + ",\"cliente\":" + quote(a.getCliente())
            + ",\"telefone\":" + quote(a.getTelefone()) + ",\"servico\":" + quote(a.getServico().getDescricao())
            + ",\"servicoId\":" + quote(a.getServico().name())
            + ",\"duracao\":" + a.getServico().getMinutos()
            + ",\"barbeiroId\":" + quote(a.getBarbeiro().name())
            + ",\"barbeiro\":" + quote(a.getBarbeiro().getNome()) + ",\"inicio\":" + quote(a.getInicio().toString())
            + ",\"fim\":" + quote(a.getFim().toString()) + ",\"preco\":" + a.getServico().getPreco() + "}";
    }
    static String lista(List<Agendamento> dados) {
        return dados.stream().map(JsonPresenter::agendamento).collect(Collectors.joining(",", "[", "]"));
    }
    static String catalogo() {
        String servicos = java.util.Arrays.stream(Servico.values()).map(s -> "{\"id\":" + quote(s.name())
            + ",\"nome\":" + quote(s.getDescricao()) + ",\"minutos\":" + s.getMinutos() + ",\"preco\":" + s.getPreco() + "}")
            .collect(Collectors.joining(",", "[", "]"));
        String barbeiros = java.util.Arrays.stream(Barbeiro.values()).map(b -> "{\"id\":" + quote(b.name())
            + ",\"nome\":" + quote(b.getNome()) + "}").collect(Collectors.joining(",", "[", "]"));
        return "{\"servicos\":" + servicos + ",\"barbeiros\":" + barbeiros + "}";
    }
}
