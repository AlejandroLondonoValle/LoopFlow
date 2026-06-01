package com.loopflow.dao.impl;

import com.loopflow.dao.HabitDAO;
import com.loopflow.model.Habit;
import com.loopflow.model.enums.Frequency;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC de {@link HabitDAO}.
 * Las consultas de lista usan JOIN con categories para enriquecer la respuesta.
 */
public class HabitDAOImpl implements HabitDAO {

    private final Connection connection;

    public HabitDAOImpl(Connection connection) {
        this.connection = connection;
    }

    // --- Mapping ---

    private static final String SELECT_WITH_CATEGORY =
        "SELECT h.id, h.category_id, h.name, h.description, h.frequency, " +
        "       h.goal_days, h.start_date, h.is_active, h.created_at, " +
        "       c.name AS category_name, c.color AS category_color, c.icon AS category_icon " +
        "FROM habits h " +
        "INNER JOIN categories c ON h.category_id = c.id ";

    private Habit mapRow(ResultSet rs) throws SQLException {
        Habit h = new Habit();
        h.setId(rs.getInt("id"));
        h.setCategoryId(rs.getInt("category_id"));
        h.setName(rs.getString("name"));
        h.setDescription(rs.getString("description"));
        h.setFrequency(Frequency.valueOf(rs.getString("frequency")));
        h.setGoalDays(rs.getInt("goal_days"));
        Date startDate = rs.getDate("start_date");
        if (startDate != null) h.setStartDate(startDate.toLocalDate());
        h.setActive(rs.getBoolean("is_active"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) h.setCreatedAt(ts.toLocalDateTime());
        // Campos de JOIN (pueden no estar presentes en todas las queries)
        try { h.setCategoryName(rs.getString("category_name")); } catch (SQLException ignored) {}
        try { h.setCategoryColor(rs.getString("category_color")); } catch (SQLException ignored) {}
        try { h.setCategoryIcon(rs.getString("category_icon")); } catch (SQLException ignored) {}
        return h;
    }

    // --- CRUD ---

    @Override
    public List<Habit> findAll() {
        List<Habit> list = new ArrayList<>();
        String sql = SELECT_WITH_CATEGORY + "ORDER BY h.created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener hábitos", e);
        }
        return list;
    }

    @Override
    public List<Habit> findActive() {
        List<Habit> list = new ArrayList<>();
        String sql = SELECT_WITH_CATEGORY + "WHERE h.is_active = 1 ORDER BY h.created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener hábitos activos", e);
        }
        return list;
    }

    @Override
    public List<Habit> findByCategory(int categoryId) {
        List<Habit> list = new ArrayList<>();
        String sql = SELECT_WITH_CATEGORY + "WHERE h.category_id = ? ORDER BY h.name ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener hábitos por categoría", e);
        }
        return list;
    }

    @Override
    public Optional<Habit> findById(int id) {
        String sql = SELECT_WITH_CATEGORY + "WHERE h.id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar hábito por id=" + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Habit save(Habit habit) {
        String sql = "INSERT INTO habits (category_id, name, description, frequency, goal_days, start_date, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, habit.getCategoryId());
            ps.setString(2, habit.getName());
            ps.setString(3, habit.getDescription());
            ps.setString(4, habit.getFrequency().name());
            ps.setInt(5, habit.getGoalDays());
            ps.setDate(6, Date.valueOf(habit.getStartDate()));
            ps.setBoolean(7, habit.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getInt(1))
                            .orElseThrow(() -> new RuntimeException("No se pudo recuperar el hábito guardado"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar hábito: " + habit.getName(), e);
        }
        throw new RuntimeException("No se generó un ID para el hábito");
    }

    @Override
    public boolean update(Habit habit) {
        String sql = "UPDATE habits SET category_id=?, name=?, description=?, frequency=?, " +
                     "goal_days=?, start_date=?, is_active=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, habit.getCategoryId());
            ps.setString(2, habit.getName());
            ps.setString(3, habit.getDescription());
            ps.setString(4, habit.getFrequency().name());
            ps.setInt(5, habit.getGoalDays());
            ps.setDate(6, Date.valueOf(habit.getStartDate()));
            ps.setBoolean(7, habit.isActive());
            ps.setInt(8, habit.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar hábito id=" + habit.getId(), e);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM habits WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar hábito id=" + id, e);
        }
    }

    @Override
    public boolean setActive(int id, boolean isActive) {
        String sql = "UPDATE habits SET is_active = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBoolean(1, isActive);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al cambiar estado de hábito id=" + id, e);
        }
    }
}
