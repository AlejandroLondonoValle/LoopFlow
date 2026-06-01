package com.loopflow.service;

import com.loopflow.dao.TaskDAO;
import com.loopflow.dao.TaskHistoryDAO;
import com.loopflow.model.Task;
import com.loopflow.model.TaskHistory;
import com.loopflow.model.enums.Priority;
import com.loopflow.model.enums.TaskStatus;

import java.util.List;

/**
 * Servicio de lógica de negocio para {@link Task} y {@link TaskHistory}.
 *
 * <p>El método clave es {@link #moveTask(int, TaskStatus)}, que mueve una tarea
 * entre columnas del tablero Kanban y registra automáticamente el historial.
 */
public class TaskService {

    private final TaskDAO taskDAO;
    private final TaskHistoryDAO taskHistoryDAO;

    public TaskService(TaskDAO taskDAO, TaskHistoryDAO taskHistoryDAO) {
        this.taskDAO = taskDAO;
        this.taskHistoryDAO = taskHistoryDAO;
    }

    // ===========================
    // CRUD de Tareas
    // ===========================

    /** Retorna todas las tareas ordenadas por prioridad y fecha. */
    public List<Task> getAllTasks() {
        return taskDAO.findAll();
    }

    /** Retorna tareas filtradas por estado (columna Kanban). */
    public List<Task> getTasksByStatus(TaskStatus status) {
        return taskDAO.findByStatus(status);
    }

    /** Retorna tareas filtradas por prioridad. */
    public List<Task> getTasksByPriority(Priority priority) {
        return taskDAO.findByPriority(priority);
    }

    /**
     * Busca una tarea por ID.
     *
     * @throws IllegalArgumentException si no existe
     */
    public Task getTaskById(int id) {
        return taskDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada con id=" + id));
    }

    /**
     * Crea una nueva tarea y registra su creación en el historial.
     *
     * @param task datos de la tarea (sin id)
     * @return la tarea creada con su ID
     */
    public Task createTask(Task task) {
        validateTask(task);
        if (task.getStatus() == null) task.setStatus(TaskStatus.TODO);
        if (task.getPriority() == null) task.setPriority(Priority.MEDIUM);

        Task saved = taskDAO.save(task);

        // Registrar creación en historial (old_status = null → nueva tarea)
        taskHistoryDAO.save(new TaskHistory(saved.getId(), null, saved.getStatus(),
                "Tarea creada"));

        return saved;
    }

    /**
     * Actualiza los datos de una tarea existente.
     * Si el estado cambió, registra el cambio en el historial.
     *
     * @param id   ID de la tarea
     * @param task nuevos datos
     * @return la tarea actualizada
     */
    public Task updateTask(int id, Task task) {
        validateTask(task);
        Task existing = getTaskById(id);
        task.setId(id);

        // Detectar cambio de estado para el historial
        boolean statusChanged = existing.getStatus() != task.getStatus();
        TaskStatus oldStatus = existing.getStatus();

        if (!taskDAO.update(task)) {
            throw new RuntimeException("No se pudo actualizar la tarea id=" + id);
        }

        if (statusChanged && task.getStatus() != null) {
            taskHistoryDAO.save(new TaskHistory(id, oldStatus, task.getStatus(),
                    "Estado actualizado durante edición"));
        }

        return getTaskById(id);
    }

    /**
     * Mueve una tarea a una nueva columna del tablero Kanban.
     *
     * <p>Proceso transaccional:
     * <ol>
     *   <li>Obtiene el estado actual de la tarea.</li>
     *   <li>Actualiza el estado en la tabla tasks.</li>
     *   <li>Registra el cambio en task_history.</li>
     * </ol>
     *
     * @param taskId    ID de la tarea a mover
     * @param newStatus nuevo estado (columna destino)
     * @return la tarea con el estado actualizado
     * @throws IllegalArgumentException si la tarea no existe o el estado no cambió
     */
    public Task moveTask(int taskId, TaskStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("El nuevo estado no puede ser nulo");
        }

        Task task = getTaskById(taskId);
        TaskStatus oldStatus = task.getStatus();

        if (oldStatus == newStatus) {
            throw new IllegalArgumentException(
                "La tarea ya se encuentra en el estado " + newStatus.name());
        }

        // 1. Actualizar estado en BD
        if (!taskDAO.updateStatus(taskId, newStatus)) {
            throw new RuntimeException("No se pudo actualizar el estado de la tarea id=" + taskId);
        }

        // 2. Registrar en historial
        String notes = String.format("Movida de %s a %s", oldStatus.name(), newStatus.name());
        taskHistoryDAO.save(new TaskHistory(taskId, oldStatus, newStatus, notes));

        // 3. Retornar tarea actualizada
        return getTaskById(taskId);
    }

    /**
     * Elimina una tarea. El historial se elimina en cascada.
     *
     * @param id ID de la tarea
     */
    public void deleteTask(int id) {
        getTaskById(id);
        if (!taskDAO.delete(id)) {
            throw new RuntimeException("No se pudo eliminar la tarea id=" + id);
        }
    }

    // ===========================
    // Historial
    // ===========================

    /**
     * Retorna el historial completo de cambios de estado de una tarea.
     *
     * @param taskId ID de la tarea
     * @return lista de entradas de historial ordenadas por fecha ascendente
     */
    public List<TaskHistory> getTaskHistory(int taskId) {
        getTaskById(taskId); // Verifica que la tarea existe
        return taskHistoryDAO.findByTask(taskId);
    }

    // ===========================
    // Validaciones
    // ===========================

    private void validateTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("La tarea no puede ser nula");
        }
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new IllegalArgumentException("El título de la tarea no puede estar vacío");
        }
        if (task.getTitle().length() > 300) {
            throw new IllegalArgumentException("El título no puede superar 300 caracteres");
        }
    }
}
