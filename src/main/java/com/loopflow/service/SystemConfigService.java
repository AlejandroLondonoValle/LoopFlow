package com.loopflow.service;

import com.loopflow.dao.SystemConfigDAO;
import com.loopflow.model.SystemConfig;

import java.util.List;

/**
 * Servicio de lógica de negocio para {@link SystemConfig}.
 */
public class SystemConfigService {

    private final SystemConfigDAO systemConfigDAO;

    public SystemConfigService(SystemConfigDAO systemConfigDAO) {
        this.systemConfigDAO = systemConfigDAO;
    }

    /** Retorna todas las configuraciones del sistema. */
    public List<SystemConfig> getAllConfigs() {
        return systemConfigDAO.findAll();
    }

    /**
     * Busca una configuración por clave.
     *
     * @param key clave de configuración
     * @return la configuración encontrada
     * @throws IllegalArgumentException si no existe la clave
     */
    public SystemConfig getConfigByKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("La clave de configuración no puede estar vacía");
        }
        return systemConfigDAO.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada: " + key));
    }

    /**
     * Retorna el valor de una configuración, o un valor por defecto si no existe.
     *
     * @param key          clave de configuración
     * @param defaultValue valor por defecto
     * @return valor de la configuración o defaultValue
     */
    public String getConfigValue(String key, String defaultValue) {
        return systemConfigDAO.findByKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    /**
     * Crea o actualiza una configuración (UPSERT).
     *
     * @param key         clave
     * @param value       nuevo valor
     * @param description descripción opcional (null para no cambiarla)
     * @return la configuración resultante
     */
    public SystemConfig setConfig(String key, String value, String description) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("La clave no puede estar vacía");
        }
        if (value == null) {
            throw new IllegalArgumentException("El valor de configuración no puede ser nulo");
        }
        SystemConfig config = new SystemConfig(key, value, description);
        return systemConfigDAO.upsert(config);
    }

    /**
     * Elimina una configuración por clave.
     *
     * @param key clave a eliminar
     * @throws IllegalArgumentException si no existe
     */
    public void deleteConfig(String key) {
        getConfigByKey(key); // Verifica existencia
        if (!systemConfigDAO.deleteByKey(key)) {
            throw new RuntimeException("No se pudo eliminar la configuración: " + key);
        }
    }
}
