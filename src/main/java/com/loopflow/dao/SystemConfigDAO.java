package com.loopflow.dao;

import com.loopflow.model.SystemConfig;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la entidad {@link SystemConfig}.
 */
public interface SystemConfigDAO {

    /** Retorna todas las configuraciones del sistema ordenadas por clave. */
    List<SystemConfig> findAll();

    /**
     * Busca una configuración por su clave única.
     *
     * @param key clave de configuración
     * @return la configuración si existe
     */
    Optional<SystemConfig> findByKey(String key);

    /**
     * Crea o actualiza una configuración (UPSERT por config_key).
     * Si la clave ya existe, actualiza el valor. Si no, la crea.
     *
     * @param config entidad con la clave y el nuevo valor
     * @return la configuración resultante
     */
    SystemConfig upsert(SystemConfig config);

    /**
     * Elimina una configuración por su clave.
     *
     * @param key clave de configuración
     * @return true si se eliminó al menos una fila
     */
    boolean deleteByKey(String key);
}
