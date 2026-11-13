package br.com.navalha.application.service;

import br.com.navalha.application.dto.CriarAgendamentoCommand;
import br.com.navalha.application.exception.*;
import br.com.navalha.application.port.AgendamentoRepository;
import br.com.navalha.domain.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/** Casos de uso. Depende apenas do domínio e do contrato de persistência. */
public final class AgendamentoService {
    private final AgendamentoRepository repository;
    private final Clock clock;

    public AgendamentoService(AgendamentoRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }
    // Serializa os casos de uso nesta instância (servidor de um único processo).
    public synchronized Agendamento criar(CriarAgendamentoCommand command) {
        if (command == null) throw regra("Informe os dados do agendamento.");
        String cliente = texto(command.cliente).replaceAll("\\s+", " ");
        String telefone = texto(command.telefone).replaceAll("[ ()+.-]", "");
        if (cliente.length() < 3 || cliente.length() > 80) throw regra("O nome deve ter de 3 a 80 caracteres.");
        if (!telefone.matches("[0-9]{10,11}")) throw regra("Informe telefone com DDD, de 10 ou 11 dígitos.");
        Servico servico = servico(command.servico);
        Barbeiro barbeiro = barbeiro(command.barbeiro);
        LocalDate data = data(command.data);
        LocalTime horario;
        try {
            if (!texto(command.horario).matches("[0-9]{2}:[0-9]{2}")) throw regra("Horário inválido.");
            horario = LocalTime.parse(command.horario);
        } catch (DateTimeParseException e) { throw regra("Horário inválido."); }
        LocalDateTime inicio = LocalDateTime.of(data, horario);
        validarHorario(inicio, servico);
        Agendamento novo = new Agendamento(UUID.randomUUID(), cliente, telefone, servico, barbeiro, inicio);
        if (repository.listarPorBarbeiroNoDia(barbeiro, data).stream().anyMatch(novo::conflitaCom))
            throw regra("Este barbeiro já possui atendimento nesse intervalo.");
        repository.salvar(novo);
        return novo;
    }
    public synchronized List<Agendamento> listar(String filtroData) {
        LocalDate filtro = texto(filtroData).isEmpty() ? null : data(filtroData);
        return repository.listar().stream()
                .filter(a -> filtro == null || a.getInicio().toLocalDate().equals(filtro))
                .sorted(Comparator.comparing(Agendamento::getInicio)).collect(Collectors.toList());
    }
    public synchronized List<LocalTime> horarios(String dataTexto, String servicoTexto, String barbeiroTexto) {
        LocalDate dia = data(dataTexto);
        Servico servico = servico(servicoTexto);
        Barbeiro barbeiro = barbeiro(barbeiroTexto);
        List<LocalTime> livres = new ArrayList<>();
        List<Agendamento> existentes = repository.listarPorBarbeiroNoDia(barbeiro, dia);
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime inicio = dia.atTime(Expediente.ABERTURA);
        while (Expediente.atende(inicio, servico)) {
            if (inicio.isAfter(agora)) {
                Agendamento candidato = new Agendamento(UUID.randomUUID(), "Consulta", "", servico, barbeiro, inicio);
                if (existentes.stream().noneMatch(candidato::conflitaCom)) livres.add(inicio.toLocalTime());
            }
            inicio = inicio.plusMinutes(Expediente.PASSO_MINUTOS);
        }
        return livres;
    }
    public synchronized void cancelar(String id) {
        UUID uuid;
        try { uuid = UUID.fromString(texto(id)); }
        catch (IllegalArgumentException e) { throw regra("Identificador inválido."); }
        if (!repository.excluir(uuid)) throw new NaoEncontradoException("Agendamento não encontrado.");
    }
    private void validarHorario(LocalDateTime inicio, Servico servico) {
        if (!inicio.isAfter(LocalDateTime.now(clock))) throw regra("Escolha uma data e um horário futuros.");
        if (!Expediente.atende(inicio, servico))
            throw regra("Atendemos de segunda a sábado, das 09h às 19h, com inícios a cada 30 minutos e término até 19h.");
    }
    private LocalDate data(String valor) {
        try { return LocalDate.parse(texto(valor)); }
        catch (DateTimeParseException e) { throw regra("Data inválida."); }
    }
    private Servico servico(String valor) {
        try { return Servico.valueOf(texto(valor)); }
        catch (IllegalArgumentException e) { throw regra("Serviço inválido."); }
    }
    private Barbeiro barbeiro(String valor) {
        try { return Barbeiro.valueOf(texto(valor)); }
        catch (IllegalArgumentException e) { throw regra("Barbeiro inválido."); }
    }
    private String texto(String valor) { return valor == null ? "" : valor.trim(); }
    private RegraNegocioException regra(String mensagem) { return new RegraNegocioException(mensagem); }
}
