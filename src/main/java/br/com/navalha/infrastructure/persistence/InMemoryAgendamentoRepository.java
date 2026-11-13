package br.com.navalha.infrastructure.persistence;

import br.com.navalha.application.port.AgendamentoRepository;
import br.com.navalha.domain.Agendamento;
import br.com.navalha.domain.Barbeiro;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Adaptador de dados. Não valida regras e não conhece HTTP. */
public final class InMemoryAgendamentoRepository implements AgendamentoRepository {
    private final Map<UUID, Agendamento> dados = new LinkedHashMap<>();
    @Override public synchronized List<Agendamento> listar() { return new ArrayList<>(dados.values()); }
    @Override public synchronized List<Agendamento> listarPorBarbeiroNoDia(Barbeiro barbeiro, LocalDate dia) {
        return dados.values().stream()
                .filter(a -> a.getBarbeiro() == barbeiro && a.getInicio().toLocalDate().equals(dia))
                .collect(Collectors.toList());
    }
    @Override public synchronized void salvar(Agendamento a) { dados.put(a.getId(), a); }
    @Override public synchronized boolean excluir(UUID id) { return dados.remove(id) != null; }
}
