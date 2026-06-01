package com.loopflow.dao.impl;

import com.loopflow.dao.DailyLogDAO;
import com.loopflow.model.DailyLog;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC de {@link DailyLogDAO}.
 * El método {@link #markCompleted} usa INSERT ... ON DUPLICATE KEY UPDATE
 * para garantizar exactamente un registro por (habit_id, log_date).
 */
public class DailyLogDAOImpl implements DailyLogDAO {

    private final Connection connection;

    public DailyLogDAOImpl(Connection connection) {
        this.connection = connection;
    }

    // --- Mapping ---

    private DailyLog mapRow(ResultSet rs) throws SQLException {
        DailyLog log = new DailyLog();
        log.setId(rs.getInt("id"));
        log.setHabitId(rs.getInt("habit_id"));
        Date d = rs.getDate("log_date");
        if (d != null) log.setLogDate(d.toLocalDate());
        log.setCompleted(rs.getBoolean("completed"));
        log.setNotes(rs.getString("notes"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) log.setCreatedAt(ts.toLocalDateTime());
        return log;
    }

    // --- Queries ---

    @Override
    public List<DailyLog> findByHabit(int habitId) {
        List<DailyLog> list = new ArrayList<>();
        String sql = "SELECT id, habit_id, log_date, completed, notes, created_at " +
                     "FROM daily_logs WHERE habit_id = ? ORDER BY log_date DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, habitId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener registros del hábito id=" + habitId, e);
        }
        return list;
    }

    @Override
    public Optional<DailyLog> findByHabitAndDate(int habitId, LocalDate date) {
        String sql = "SELECT id, habit_id, log_date, completed, notes, created_at " +
                     "FROM daily_logs WHERE habit_id = ? AND log_date = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, habitId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar registro por hábito y fecha", e);
        }
        return Optional.empty();
    }

    @Override
    public List<DailyLog> findByHabitAndDateRange(int habitId, LocalDate from, LocalDate to) {
        List<DailyLog> list = new ArrayList<>();
        String sql = "SELECT id, habit_id, log_date, completed, notes, created_at " +
                     "FROM daily_logs WHERE habit_id = ? AND log_date BETWEEN ? AND ? " +
                     "ORDER BY log_date ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, habitId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener registros en rango de fechas", e);
        }
        return list;
    }

    @Override
    public List<DailyLog> findByDate(LocalDate date) {
        List<DailyLog> list = new ArrayList<>();
        String sql = "SELECT id, habit_id, log_date, completed, notes, created_at " +
                     "FROM daily_logs WHERE log_date = ? ORDER BY habit_id ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener registros por fecha " + date, e);
        }
        return list;
    }

    @Override
    public DailyLog markCompleted(int habitId, LocalDate date, boolean completed, String notes) {
        // UPSERT: inserta si no existe, actualiza si ya existe (clave única habit_id + log_date)
        String sql = "INSERT INTO daily_logs (habit_id, log_date, completed, notes) VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE completed = VALUES(completed), notes = VALUES(notes)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, habitId);
            ps.setDate(2, Date.valueOf(date));
            ps.setBoolean(3, completed);
            ps.setString(4, notes);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al marcar hábito como completado", e);
        }
        return findByHabitAndDate(habitId, date)
                .orElseThrow(() -> new RuntimeException("No se pudo recuperar el registro tras el UPSERT"));
    }

    @Override
    public DailyLog save(DailyLog log) {
        String sql = "INSERT INTO daily_logs (habit_id, log_date, completed, notes) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, log.getHabitId());
            ps.setDate(2, Date.valueOf(log.getLogDate()));
            ps.setBoolean(3, log.isCompleted());
            ps.setString(4, log.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findByHabitAndDate(log.getHabitId(), log.getLogDate())
                            .orElseThrow(() -> new RuntimeException("No se pudo recuperar el registro guardado"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar registro diario", e);
        }
        throw new RuntimeException("No se generó un ID para el registro");
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM daily_logs WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar registro id=" + id, e);
        }
    }
}
