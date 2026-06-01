package com.loopflow.dao.impl;

import com.loopflow.dao.SystemConfigDAO;
import com.loopflow.model.SystemConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC de {@link SystemConfigDAO}.
 * El método {@link #upsert} usa INSERT ... ON DUPLICATE KEY UPDATE
 * aprovechando el índice UNIQUE en config_key.
 */
public class SystemConfigDAOImpl implements SystemConfigDAO {

    private final Connection connection;

    public SystemConfigDAOImpl(Connection connection) {
        this.connection = connection;
    }

    // --- Mapping ---

    private SystemConfig mapRow(ResultSet rs) throws SQLException {
        SystemConfig sc = new SystemConfig();
        sc.setId(rs.getInt("id"));
        sc.setConfigKey(rs.getString("config_key"));
        sc.setConfigValue(rs.getString("config_value"));
        sc.setDescription(rs.getString("description"));
        Timestamp ts = rs.getTimestamp("updated_at");
        if (ts != null) sc.setUpdatedAt(ts.toLocalDateTime());
        return sc;
    }

    // --- CRUD ---

    @Override
    public List<SystemConfig> findAll() {
        List<SystemConfig> list = new ArrayList<>();
        String sql = "SELECT id, config_key, config_value, description, updated_at " +
                     "FROM system_config ORDER BY config_key ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener configuraciones del sistema", e);
        }
        return list;
    }

    @Override
    public Optional<SystemConfig> findByKey(String key) {
        String sql = "SELECT id, config_key, config_value, description, updated_at " +
                     "FROM system_config WHERE config_key = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar configuración '" + key + "'", e);
        }
        return Optional.empty();
    }

    @Override
    public SystemConfig upsert(SystemConfig config) {
        String sql = "INSERT INTO system_config (config_key, config_value, description) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), " +
                     "description = COALESCE(VALUES(description), description)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, config.getConfigKey());
            ps.setString(2, config.getConfigValue());
            ps.setString(3, config.getDescription());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al realizar UPSERT de configuración '" + config.getConfigKey() + "'", e);
        }
        return findByKey(config.getConfigKey())
                .orElseThrow(() -> new RuntimeException("No se pudo recuperar la configuración tras el UPSERT"));
    }

    @Override
    public boolean deleteByKey(String key) {
        String sql = "DELETE FROM system_config WHERE config_key = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, key);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar configuración '" + key + "'", e);
        }
    }
}
