package br.com.navalha.infrastructure.persistence;

import br.com.navalha.application.port.AgendamentoRepository;
import br.com.navalha.domain.*;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/** Adaptador JDBC: SQL e mapeamento de registros, sem regras de agendamento. */
public final class JdbcAgendamentoRepository implements AgendamentoRepository {
    private final DataSource dataSource;

    public JdbcAgendamentoRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override public List<Agendamento> listar() {
        String sql = "SELECT id, cliente, telefone, servico, barbeiro, inicio FROM agendamento";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<Agendamento> registros = new ArrayList<>();
            while (result.next()) registros.add(new Agendamento(
                UUID.fromString(result.getString("id")), result.getString("cliente"),
                result.getString("telefone"), Servico.valueOf(result.getString("servico")),
                Barbeiro.valueOf(result.getString("barbeiro")), result.getTimestamp("inicio").toLocalDateTime()));
            return registros;
        } catch (SQLException e) { throw new IllegalStateException("Falha ao consultar agendamentos no banco.", e); }
    }

    @Override public void salvar(Agendamento agendamento) {
        String sql = "INSERT INTO agendamento (id, cliente, telefone, servico, barbeiro, inicio) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, agendamento.getId().toString());
            statement.setString(2, agendamento.getCliente());
            statement.setString(3, agendamento.getTelefone());
            statement.setString(4, agendamento.getServico().name());
            statement.setString(5, agendamento.getBarbeiro().name());
            statement.setTimestamp(6, Timestamp.valueOf(agendamento.getInicio()));
            statement.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException("Falha ao salvar agendamento no banco.", e); }
    }

    @Override public boolean excluir(UUID id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM agendamento WHERE id = ?")) {
            statement.setString(1, id.toString());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) { throw new IllegalStateException("Falha ao excluir agendamento no banco.", e); }
    }
}
