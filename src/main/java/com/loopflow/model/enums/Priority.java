package com.loopflow.model.enums;

/**
 * Nivel de prioridad de una tarea en el tablero Scrum/Kanban.
 * Mapea directamente al ENUM de MySQL en la tabla tasks.
 */
public enum Priority {
    /** Prioridad baja — puede esperar. */
    LOW,
    /** Prioridad media — atender en el corto plazo. */
    MEDIUM,
    /** Prioridad alta — atender pronto. */
    HIGH,
    /** Prioridad crítica — atención inmediata requerida. */
    CRITICAL
}
