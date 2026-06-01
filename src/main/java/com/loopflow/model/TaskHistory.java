package com.loopflow.model;

import com.loopflow.model.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad: Historial de cambios de estado de una tarea.
 * Mapea a la tabla {@code task_history} en MySQL.
 * Se crea automáticamente cada vez que una tarea se mueve entre columnas Kanban.
 */
public class TaskHistory {

    private int id;
    private int taskId;
    /** Estado anterior — puede ser null si es la creación inicial de la tarea. */
    private TaskStatus oldStatus;
    private TaskStatus newStatus;
    private LocalDateTime changedAt;
    private String notes;

    // --- Constructores ---

    public TaskHistory() {}

    public TaskHistory(int id, int taskId, TaskStatus oldStatus, TaskStatus newStatus,
                       LocalDateTime changedAt, String notes) {
        this.id = id;
        this.taskId = taskId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedAt = changedAt;
        this.notes = notes;
    }

    /** Constructor para registrar un cambio (sin id ni changedAt — se asigna en BD). */
    public TaskHistory(int taskId, TaskStatus oldStatus, TaskStatus newStatus, String notes) {
        this.taskId = taskId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.notes = notes;
    }

    // --- Getters y Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public TaskStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(TaskStatus oldStatus) { this.oldStatus = oldStatus; }

    public TaskStatus getNewStatus() { return newStatus; }
    public void setNewStatus(TaskStatus newStatus) { this.newStatus = newStatus; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // --- equals / hashCode / toString ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskHistory th)) return false;
        return id == th.id && taskId == th.taskId;
    }

    @Override
    public int hashCode() { return Objects.hash(id, taskId); }

    @Override
    public String toString() {
        return "TaskHistory{id=" + id + ", taskId=" + taskId +
               ", " + oldStatus + " → " + newStatus + ", at=" + changedAt + "}";
    }
}
