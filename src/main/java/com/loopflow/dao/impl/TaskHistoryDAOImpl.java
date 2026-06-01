package com.loopflow.dao.impl;

import com.loopflow.dao.TaskHistoryDAO;
import com.loopflow.model.TaskHistory;
import com.loopflow.model.enums.TaskStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de {@link TaskHistoryDAO}.
 * El historial es append-only — solo insert y read.
 */
public class TaskHistoryDAOImpl implements TaskHistoryDAO {

    private final Connection connection;

    public TaskHistoryDAOImpl(Connection connection) {
        this.connection = connection;
    }

    // --- Mapping ---

    private TaskHistory mapRow(ResultSet rs) throws SQLException {
        TaskHistory h = new TaskHistory();
        h.setId(rs.getInt("id"));
        h.setTaskId(rs.getInt("task_id"));
        String oldStatus = rs.getString("old_status");
        h.setOldStatus(oldStatus != null ? TaskStatus.valueOf(oldStatus) : null);
        h.setNewStatus(TaskStatus.valueOf(rs.getString("new_status")));
        Timestamp ts = rs.getTimestamp("changed_at");
        if (ts != null) h.setChangedAt(ts.toLocalDateTime());
        h.setNotes(rs.getString("notes"));
        return h;
    }

    // --- Operaciones ---

    @Override
    public List<TaskHistory> findByTask(int taskId) {
        List<TaskHistory> list = new ArrayList<>();
        String sql = "SELECT id, task_id, old_status, new_status, changed_at, notes " +
                     "FROM task_history WHERE task_id = ? ORDER BY changed_at ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener historial de tarea id=" + taskId, e);
        }
        return list;
    }

    @Override
    public TaskHistory save(TaskHistory history) {
        String sql = "INSERT INTO task_history (task_id, old_status, new_status, notes) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, history.getTaskId());
            if (history.getOldStatus() != null) {
                ps.setString(2, history.getOldStatus().name());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.setString(3, history.getNewStatus().name());
            ps.setString(4, history.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    history.setId(id);
                    // Recuperar el changed_at generado por la BD
                    String selectSql = "SELECT changed_at FROM task_history WHERE id = ?";
                    try (PreparedStatement ps2 = connection.prepareStatement(selectSql)) {
                        ps2.setInt(1, id);
                        try (ResultSet rs2 = ps2.executeQuery()) {
                            if (rs2.next()) {
                                Timestamp ts = rs2.getTimestamp("changed_at");
                                if (ts != null) history.setChangedAt(ts.toLocalDateTime());
                            }
                        }
                    }
                    return history;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar historial de tarea", e);
        }
        throw new RuntimeException("No se generó un ID para la entrada de historial");
    }
}
