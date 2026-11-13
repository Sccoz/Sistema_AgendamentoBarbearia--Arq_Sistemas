package br.com.navalha.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** Entidade imutável. Não conhece HTTP, armazenamento ou interface gráfica. */
public final class Agendamento {
    private final UUID id;
    private final String cliente;
    private final String telefone;
    private final Servico servico;
    private final Barbeiro barbeiro;
    private final LocalDateTime inicio;

    public Agendamento(UUID id, String cliente, String telefone, Servico servico,
                       Barbeiro barbeiro, LocalDateTime inicio) {
        this.id = Objects.requireNonNull(id);
        this.cliente = Objects.requireNonNull(cliente);
        this.telefone = Objects.requireNonNull(telefone);
        this.servico = Objects.requireNonNull(servico);
        this.barbeiro = Objects.requireNonNull(barbeiro);
        this.inicio = Objects.requireNonNull(inicio);
    }
    public UUID getId() { return id; }
    public String getCliente() { return cliente; }
    public String getTelefone() { return telefone; }
    public Servico getServico() { return servico; }
    public Barbeiro getBarbeiro() { return barbeiro; }
    public LocalDateTime getInicio() { return inicio; }
    public LocalDateTime getFim() { return inicio.plusMinutes(servico.getMinutos()); }
    public boolean conflitaCom(Agendamento outro) {
        return barbeiro == outro.barbeiro && inicio.isBefore(outro.getFim())
                && getFim().isAfter(outro.inicio);
    }
}
