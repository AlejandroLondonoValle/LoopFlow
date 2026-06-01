package com.loopflow.model.enums;

/**
 * Estado de una tarea en el tablero Scrum/Kanban.
 * Mapea directamente al ENUM de MySQL en la tabla tasks y task_history.
 */
public enum TaskStatus {
    /** Tarea pendiente, en la columna "Por Hacer". */
    TODO,
    /** Tarea en ejecución, en la columna "En Progreso". */
    IN_PROGRESS,
    /** Tarea completada, en la columna "Hecho". */
    DONE
}
