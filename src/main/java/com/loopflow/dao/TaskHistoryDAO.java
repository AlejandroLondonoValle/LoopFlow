package com.loopflow.dao;

import com.loopflow.model.TaskHistory;

import java.util.List;

/**
 * Contrato de acceso a datos para la entidad {@link TaskHistory}.
 * El historial es append-only: no se actualiza ni elimina directamente.
 * La eliminación ocurre en cascada cuando se elimina la tarea padre.
 */
public interface TaskHistoryDAO {

    /**
     * Retorna el historial completo de una tarea, ordenado por fecha ascendente
     * (el primer cambio primero).
     *
     * @param taskId ID de la tarea
     * @return lista de entradas de historial
     */
    List<TaskHistory> findByTask(int taskId);

    /**
     * Persiste una nueva entrada de historial.
     *
     * @param history entidad a guardar (sin id ni changedAt — la BD los asigna)
     * @return la entrada con el id y changedAt generados
     */
    TaskHistory save(TaskHistory history);
}
