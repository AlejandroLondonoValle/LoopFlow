package com.loopflow.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad: Configuración del sistema (clave-valor).
 * Mapea a la tabla {@code system_config} en MySQL.
 * La clave (config_key) es UNIQUE.
 */
public class SystemConfig {

    private int id;
    private String configKey;
    private String configValue;
    private String description;
    private LocalDateTime updatedAt;

    // --- Constructores ---

    public SystemConfig() {}

    public SystemConfig(int id, String configKey, String configValue,
                        String description, LocalDateTime updatedAt) {
        this.id = id;
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
        this.updatedAt = updatedAt;
    }

    /** Constructor para crear/actualizar una configuración (sin id). */
    public SystemConfig(String configKey, String configValue, String description) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
    }

    // --- Getters y Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }

    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // --- equals / hashCode / toString ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SystemConfig sc)) return false;
        return Objects.equals(configKey, sc.configKey);
    }

    @Override
    public int hashCode() { return Objects.hash(configKey); }

    @Override
    public String toString() {
        return "SystemConfig{key='" + configKey + "', value='" + configValue + "'}";
    }
}
