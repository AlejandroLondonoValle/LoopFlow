package com.loopflow.model.enums;

/**
 * Frecuencia de ejecución de un hábito.
 * Mapea directamente al ENUM de MySQL en la tabla habits.
 */
public enum Frequency {
    /** El hábito debe realizarse todos los días. */
    DAILY,
    /** El hábito debe realizarse una vez por semana. */
    WEEKLY,
    /** El hábito debe realizarse una vez por mes. */
    MONTHLY
}
