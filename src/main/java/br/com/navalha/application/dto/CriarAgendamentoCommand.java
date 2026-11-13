package br.com.navalha.application.dto;

/** Dados de entrada independentes de HTTP e HTML. */
public final class CriarAgendamentoCommand {
    public final String cliente;
    public final String telefone;
    public final String servico;
    public final String barbeiro;
    public final String data;
    public final String horario;

    public CriarAgendamentoCommand(String cliente, String telefone, String servico,
                                   String barbeiro, String data, String horario) {
        this.cliente = cliente; this.telefone = telefone; this.servico = servico;
        this.barbeiro = barbeiro; this.data = data; this.horario = horario;
    }
}
