package br.com.navalha.application.port;

import br.com.navalha.domain.Agendamento;
import java.util.List;
import java.util.UUID;

/** Contrato da aplicação; a infraestrutura implementa este contrato. */
public interface AgendamentoRepository {
    List<Agendamento> listar();
    void salvar(Agendamento agendamento);
    boolean excluir(UUID id);
}
