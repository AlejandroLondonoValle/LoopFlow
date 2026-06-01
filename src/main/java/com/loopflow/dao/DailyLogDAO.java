package com.loopflow.dao;

import com.loopflow.model.DailyLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la entidad {@link DailyLog}.
 */
public interface DailyLogDAO {

    /** Retorna todos los registros de un hábito, ordenados por fecha descendente. */
    List<DailyLog> findByHabit(int habitId);

    /** Busca el registro de un hábito para una fecha específica. */
    Optional<DailyLog> findByHabitAndDate(int habitId, LocalDate date);

    /**
     * Retorna los registros de un hábito dentro de un rango de fechas (ambos inclusive).
     *
     * @param habitId  ID del hábito
     * @param from     fecha de inicio (inclusive)
     * @param to       fecha de fin (inclusive)
     * @return lista de registros en el rango
     */
    List<DailyLog> findByHabitAndDateRange(int habitId, LocalDate from, LocalDate to);

    /**
     * Retorna todos los registros de todos los hábitos para una fecha concreta.
     * Útil para el dashboard del día.
     *
     * @param date fecha del día
     * @return lista de registros del día
     */
    List<DailyLog> findByDate(LocalDate date);

    /**
     * Marca un hábito como completado o no completado para una fecha.
     * Realiza un UPSERT: INSERT si no existe, UPDATE si ya existe.
     *
     * @param habitId   ID del hábito
     * @param date      fecha del registro
     * @param completed true = completado, false = pendiente
     * @param notes     notas opcionales
     * @return el registro resultante
     */
    DailyLog markCompleted(int habitId, LocalDate date, boolean completed, String notes);

    /**
     * Guarda un nuevo registro diario.
     *
     * @param log entidad a guardar (sin id)
     * @return el registro con el id generado
     */
    DailyLog save(DailyLog log);

    /**
     * Elimina un registro por su ID.
     *
     * @param id ID del registro
     * @return true si se eliminó al menos una fila
     */
    boolean delete(int id);
}
