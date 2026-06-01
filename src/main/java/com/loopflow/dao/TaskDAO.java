package com.loopflow.dao;

import com.loopflow.model.Task;
import com.loopflow.model.enums.Priority;
import com.loopflow.model.enums.TaskStatus;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la entidad {@link Task}.
 */
public interface TaskDAO {

    /** Retorna todas las tareas ordenadas por prioridad descendente y fecha de creación. */
    List<Task> findAll();

    /** Retorna todas las tareas con un estado específico. */
    List<Task> findByStatus(TaskStatus status);

    /** Retorna todas las tareas con una prioridad específica. */
    List<Task> findByPriority(Priority priority);

    /** Busca una tarea por su ID. */
    Optional<Task> findById(int id);

    /**
     * Persiste una nueva tarea en la base de datos.
     *
     * @param task entidad a guardar (sin id)
     * @return la tarea con el id generado
     */
    Task save(Task task);

    /**
     * Actualiza completamente una tarea existente.
     *
     * @param task entidad con datos actualizados (debe tener id válido)
     * @return true si se actualizó al menos una fila
     */
    boolean update(Task task);

    /**
     * Actualiza únicamente el estado de una tarea.
     * Usado internamente por {@code TaskService.moveTask()}.
     *
     * @param id        ID de la tarea
     * @param newStatus nuevo estado
     * @return true si se actualizó correctamente
     */
    boolean updateStatus(int id, TaskStatus newStatus);

    /**
     * Elimina una tarea por su ID.
     * El historial asociado se elimina en cascada.
     *
     * @param id ID de la tarea
     * @return true si se eliminó al menos una fila
     */
    boolean delete(int id);
}
