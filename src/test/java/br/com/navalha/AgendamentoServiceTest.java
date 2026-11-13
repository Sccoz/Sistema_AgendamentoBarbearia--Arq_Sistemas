package br.com.navalha;
import br.com.navalha.application.dto.CriarAgendamentoCommand;
import br.com.navalha.application.service.AgendamentoService;
import br.com.navalha.application.exception.*;
import br.com.navalha.infrastructure.persistence.InMemoryAgendamentoRepository;
import java.time.*;

public final class AgendamentoServiceTest {
    private static CriarAgendamentoCommand cmd(String nome, String data, String hora, String servico, String barbeiro) {
        return new CriarAgendamentoCommand(nome, "11999999999", servico, barbeiro, data, hora);
    }
    private static void check(boolean result) { if (!result) throw new AssertionError("Resultado inesperado"); }
    private static void rejeita(Runnable action) {
        try { action.run(); } catch (RegraNegocioException expected) { return; }
        throw new AssertionError("Deveria rejeitar entrada");
    }
    public static void main(String[] args) {
        AgendamentoService s = new AgendamentoService(new InMemoryAgendamentoRepository(),
            Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZoneId.of("America/Sao_Paulo")));
        String dia = "2026-11-16";
        String id = s.criar(cmd("João Silva", dia, "10:00", "COMBO", "ANDRE")).getId().toString();
        check(s.listar(dia).size() == 1);
        rejeita(() -> s.criar(cmd("Ana", dia, "10:30", "CORTE", "ANDRE")));
        s.criar(cmd("Ana", dia, "10:30", "CORTE", "BRUNO"));
        s.criar(cmd("Luiz", dia, "11:00", "CORTE", "ANDRE"));
        rejeita(() -> s.criar(cmd("A", dia, "12:00", "CORTE", "ANDRE")));
        rejeita(() -> s.criar(cmd("Ana", "2026-11-15", "12:00", "CORTE", "ANDRE")));
        rejeita(() -> s.criar(cmd("Ana", "2020-01-01", "12:00", "CORTE", "ANDRE")));
        rejeita(() -> s.criar(cmd("Ana", "2026-02-30", "12:00", "CORTE", "ANDRE")));
        rejeita(() -> s.criar(cmd("Ana", dia, "18:30", "COMBO", "ANDRE")));
        rejeita(() -> s.criar(cmd("Ana", dia, "08:30", "CORTE", "ANDRE")));
        rejeita(() -> s.criar(cmd("Ana", dia, "12:15", "CORTE", "ANDRE")));
        rejeita(() -> s.criar(cmd("Ana", dia, "12:00", "INVALIDO", "ANDRE")));
        rejeita(() -> s.criar(new CriarAgendamentoCommand("Ana", "123", "CORTE", "ANDRE", dia, "12:00")));
        check(!s.horarios(dia,"CORTE","ANDRE").contains(LocalTime.of(10,30)));
        check(s.horarios(dia,"COMBO","ANDRE").get(s.horarios(dia,"COMBO","ANDRE").size()-1).equals(LocalTime.of(18,0)));
        s.cancelar(id);
        check(s.horarios(dia,"CORTE","ANDRE").contains(LocalTime.of(10,30)));
        try { s.cancelar(id); throw new AssertionError("Deveria retornar inexistente"); }
        catch (NaoEncontradoException expected) { }
        System.out.println("Testes de negocio aprovados.");
    }
}
