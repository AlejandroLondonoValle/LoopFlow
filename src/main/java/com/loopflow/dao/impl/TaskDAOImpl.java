package com.loopflow.dao.impl;

import com.loopflow.dao.TaskDAO;
import com.loopflow.model.Task;
import com.loopflow.model.enums.Priority;
import com.loopflow.model.enums.TaskStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC de {@link TaskDAO}.
 */
public class TaskDAOImpl implements TaskDAO {

    private final Connection connection;

    public TaskDAOImpl(Connection connection) {
        this.connection = connection;
    }

    // --- Mapping ---

    private static final String SELECT_ALL =
        "SELECT id, title, description, status, priority, due_date, created_at, updated_at FROM tasks ";

    private Task mapRow(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setId(rs.getInt("id"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        t.setStatus(TaskStatus.valueOf(rs.getString("status")));
        t.setPriority(Priority.valueOf(rs.getString("priority")));
        Date dueDate = rs.getDate("due_date");
        if (dueDate != null) t.setDueDate(dueDate.toLocalDate());
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) t.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) t.setUpdatedAt(updated.toLocalDateTime());
        return t;
    }

    // --- CRUD ---

    @Override
    public List<Task> findAll() {
        List<Task> list = new ArrayList<>();
        String sql = SELECT_ALL +
            "ORDER BY FIELD(priority,'CRITICAL','HIGH','MEDIUM','LOW'), created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            // FIELD() es MySQL-específico; fallback para H2 en tests
            String sqlFallback = SELECT_ALL + "ORDER BY created_at DESC";
            try (PreparedStatement ps2 = connection.prepareStatement(sqlFallback);
                 ResultSet rs2 = ps2.executeQuery()) {
                while (rs2.next()) list.add(mapRow(rs2));
            } catch (SQLException ex) {
                throw new RuntimeException("Error al obtener tareas", ex);
            }
        }
        return list;
    }

    @Override
    public List<Task> findByStatus(TaskStatus status) {
        List<Task> list = new ArrayList<>();
        String sql = SELECT_ALL + "WHERE status = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener tareas por estado", e);
        }
        return list;
    }

    @Override
    public List<Task> findByPriority(Priority priority) {
        List<Task> list = new ArrayList<>();
        String sql = SELECT_ALL + "WHERE priority = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, priority.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener tareas por prioridad", e);
        }
        return list;
    }

    @Override
    public Optional<Task> findById(int id) {
        String sql = SELECT_ALL + "WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar tarea por id=" + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Task save(Task task) {
        String sql = "INSERT INTO tasks (title, description, status, priority, due_date) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, (task.getStatus() != null ? task.getStatus() : TaskStatus.TODO).name());
            ps.setString(4, (task.getPriority() != null ? task.getPriority() : Priority.MEDIUM).name());
            if (task.getDueDate() != null) ps.setDate(5, Date.valueOf(task.getDueDate()));
            else ps.setNull(5, Types.DATE);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getInt(1))
                            .orElseThrow(() -> new RuntimeException("No se pudo recuperar la tarea guardada"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar tarea: " + task.getTitle(), e);
        }
        throw new RuntimeException("No se generó un ID para la tarea");
    }

    @Override
    public boolean update(Task task) {
        String sql = "UPDATE tasks SET title=?, description=?, status=?, priority=?, due_date=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getStatus().name());
            ps.setString(4, task.getPriority().name());
            if (task.getDueDate() != null) ps.setDate(5, Date.valueOf(task.getDueDate()));
            else ps.setNull(5, Types.DATE);
            ps.setInt(6, task.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar tarea id=" + task.getId(), e);
        }
    }

    @Override
    public boolean updateStatus(int id, TaskStatus newStatus) {
        String sql = "UPDATE tasks SET status = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar estado de tarea id=" + id, e);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar tarea id=" + id, e);
        }
    }
}
