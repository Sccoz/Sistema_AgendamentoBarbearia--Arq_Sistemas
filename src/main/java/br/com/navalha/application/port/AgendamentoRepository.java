package br.com.navalha.application.port;

import br.com.navalha.domain.Agendamento;
import br.com.navalha.domain.Barbeiro;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Contrato da aplicação; a infraestrutura implementa este contrato. */
public interface AgendamentoRepository {
    List<Agendamento> listar();
    List<Agendamento> listarPorBarbeiroNoDia(Barbeiro barbeiro, LocalDate dia);
    void salvar(Agendamento agendamento);
    boolean excluir(UUID id);
}
