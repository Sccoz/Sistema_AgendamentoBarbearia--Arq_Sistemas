package br.com.navalha.infrastructure.persistence;

import br.com.navalha.application.port.AgendamentoRepository;
import br.com.navalha.domain.*;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Adaptador JDBC: SQL e mapeamento de registros, sem regras de agendamento. */
public final class JdbcAgendamentoRepository implements AgendamentoRepository {
    private final DataSource dataSource;

    public JdbcAgendamentoRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    private static final String SELECT = "SELECT id, cliente, telefone, servico, barbeiro, inicio FROM agendamento";

    @Override public List<Agendamento> listar() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT);
             ResultSet result = statement.executeQuery()) {
            return mapear(result);
        } catch (SQLException e) { throw new IllegalStateException("Falha ao consultar agendamentos no banco.", e); }
    }

    @Override public List<Agendamento> listarPorBarbeiroNoDia(Barbeiro barbeiro, LocalDate dia) {
        String sql = SELECT + " WHERE barbeiro = ? AND inicio >= ? AND inicio < ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, barbeiro.name());
            statement.setTimestamp(2, Timestamp.valueOf(dia.atStartOfDay()));
            statement.setTimestamp(3, Timestamp.valueOf(dia.plusDays(1).atStartOfDay()));
            try (ResultSet result = statement.executeQuery()) { return mapear(result); }
        } catch (SQLException e) { throw new IllegalStateException("Falha ao consultar agendamentos do barbeiro.", e); }
    }

    private List<Agendamento> mapear(ResultSet result) throws SQLException {
        List<Agendamento> registros = new ArrayList<>();
        while (result.next()) registros.add(new Agendamento(
            UUID.fromString(result.getString("id")), result.getString("cliente"),
            result.getString("telefone"), Servico.valueOf(result.getString("servico")),
            Barbeiro.valueOf(result.getString("barbeiro")), result.getTimestamp("inicio").toLocalDateTime()));
        return registros;
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
