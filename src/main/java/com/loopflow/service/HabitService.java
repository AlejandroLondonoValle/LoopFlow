package com.loopflow.service;

import com.loopflow.dao.DailyLogDAO;
import com.loopflow.dao.HabitDAO;
import com.loopflow.model.DailyLog;
import com.loopflow.model.Habit;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de lógica de negocio para {@link Habit} y {@link DailyLog}.
 *
 * <p>Cálculos clave:
 * <ul>
 *   <li>{@link #calculateStreak(int)} — Racha actual de días consecutivos completados.</li>
 *   <li>{@link #getCompletionRate(int, LocalDate, LocalDate)} — Tasa de cumplimiento (%) en un rango.</li>
 * </ul>
 */
public class HabitService {

    private final HabitDAO habitDAO;
    private final DailyLogDAO dailyLogDAO;

    public HabitService(HabitDAO habitDAO, DailyLogDAO dailyLogDAO) {
        this.habitDAO = habitDAO;
        this.dailyLogDAO = dailyLogDAO;
    }

    // ===========================
    // CRUD de Hábitos
    // ===========================

    /** Retorna todos los hábitos (activos e inactivos). */
    public List<Habit> getAllHabits() {
        return habitDAO.findAll();
    }

    /** Retorna solo los hábitos activos. */
    public List<Habit> getActiveHabits() {
        return habitDAO.findActive();
    }

    /** Retorna hábitos de una categoría específica. */
    public List<Habit> getHabitsByCategory(int categoryId) {
        return habitDAO.findByCategory(categoryId);
    }

    /**
     * Busca un hábito por ID.
     *
     * @throws IllegalArgumentException si no existe
     */
    public Habit getHabitById(int id) {
        return habitDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hábito no encontrado con id=" + id));
    }

    /**
     * Crea un nuevo hábito con validaciones de negocio.
     *
     * @param habit datos del hábito (sin id)
     * @return el hábito creado con su ID
     */
    public Habit createHabit(Habit habit) {
        validateHabit(habit);
        if (habit.getStartDate() == null) {
            habit.setStartDate(LocalDate.now());
        }
        habit.setActive(true);
        return habitDAO.save(habit);
    }

    /**
     * Actualiza un hábito existente.
     *
     * @param id    ID del hábito
     * @param habit nuevos datos
     * @return el hábito actualizado
     */
    public Habit updateHabit(int id, Habit habit) {
        validateHabit(habit);
        getHabitById(id); // Verifica existencia
        habit.setId(id);
        if (!habitDAO.update(habit)) {
            throw new RuntimeException("No se pudo actualizar el hábito id=" + id);
        }
        return getHabitById(id);
    }

    /**
     * Elimina un hábito. Los registros diarios se eliminan en cascada.
     *
     * @param id ID del hábito
     */
    public void deleteHabit(int id) {
        getHabitById(id);
        if (!habitDAO.delete(id)) {
            throw new RuntimeException("No se pudo eliminar el hábito id=" + id);
        }
    }

    /**
     * Archiva (desactiva) o activa un hábito.
     *
     * @param id       ID del hábito
     * @param isActive true = activar, false = archivar
     * @return el hábito actualizado
     */
    public Habit setHabitActive(int id, boolean isActive) {
        getHabitById(id);
        habitDAO.setActive(id, isActive);
        return getHabitById(id);
    }

    // ===========================
    // Completar hábito del día
    // ===========================

    /**
     * Marca un hábito como completado (o no) para el día de hoy.
     * Si ya existe un registro para hoy, lo actualiza (UPSERT).
     *
     * @param habitId   ID del hábito
     * @param completed true = completado
     * @param notes     notas opcionales
     * @return el registro diario resultante
     */
    public DailyLog markTodayCompleted(int habitId, boolean completed, String notes) {
        getHabitById(habitId); // Verifica que el hábito existe
        return dailyLogDAO.markCompleted(habitId, LocalDate.now(), completed, notes);
    }

    /**
     * Marca un hábito como completado para una fecha específica.
     *
     * @param habitId   ID del hábito
     * @param date      fecha del registro
     * @param completed estado de completado
     * @param notes     notas opcionales
     * @return el registro diario resultante
     */
    public DailyLog markCompleted(int habitId, LocalDate date, boolean completed, String notes) {
        getHabitById(habitId);
        return dailyLogDAO.markCompleted(habitId, date, completed, notes);
    }

    /** Retorna todos los registros diarios de un hábito. */
    public List<DailyLog> getLogsForHabit(int habitId) {
        getHabitById(habitId);
        return dailyLogDAO.findByHabit(habitId);
    }

    // ===========================
    // Cálculos de estadísticas
    // ===========================

    /**
     * Calcula la racha actual de días consecutivos completados para un hábito.
     *
     * <p>Algoritmo: partiendo desde ayer, cuenta hacia atrás cuántos días
     * consecutivos el hábito fue marcado como completado. Si hoy también
     * está completado, lo incluye.
     *
     * @param habitId ID del hábito
     * @return número de días consecutivos completados (≥ 0)
     */
    public int calculateStreak(int habitId) {
        Habit habit = getHabitById(habitId);
        LocalDate today = LocalDate.now();
        LocalDate checkDate = today;
        int streak = 0;

        // Obtener logs desde la fecha de inicio del hábito hasta hoy
        List<DailyLog> logs = dailyLogDAO.findByHabitAndDateRange(habitId, habit.getStartDate(), today);

        // Convertir a mapa fecha→completado para búsqueda O(1)
        Map<LocalDate, Boolean> completionMap = logs.stream()
                .collect(Collectors.toMap(DailyLog::getLogDate, DailyLog::isCompleted));

        // Contar días consecutivos hacia atrás desde hoy
        while (!checkDate.isBefore(habit.getStartDate())) {
            Boolean completed = completionMap.get(checkDate);
            if (Boolean.TRUE.equals(completed)) {
                streak++;
                checkDate = checkDate.minusDays(1);
            } else {
                // Si hoy no está completado, empezar desde ayer
                if (checkDate.equals(today) && !Boolean.TRUE.equals(completed)) {
                    checkDate = checkDate.minusDays(1);
                    continue;
                }
                break; // Cadena rota
            }
        }
        return streak;
    }

    /**
     * Calcula la tasa de cumplimiento de un hábito en un rango de fechas.
     *
     * <p>Fórmula: (días completados / días totales del rango) × 100
     *
     * @param habitId ID del hábito
     * @param from    fecha de inicio del rango (inclusive)
     * @param to      fecha de fin del rango (inclusive)
     * @return porcentaje de cumplimiento (0.0 – 100.0), redondeado a 2 decimales
     */
    public double getCompletionRate(int habitId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior o igual a la fecha de fin");
        }
        getHabitById(habitId);

        List<DailyLog> logs = dailyLogDAO.findByHabitAndDateRange(habitId, from, to);
        long totalDays = from.until(to).getDays() + 1;
        long completedDays = logs.stream().filter(DailyLog::isCompleted).count();

        if (totalDays == 0) return 0.0;
        double rate = (double) completedDays / totalDays * 100.0;
        return Math.round(rate * 100.0) / 100.0;
    }

    // ===========================
    // Validaciones
    // ===========================

    private void validateHabit(Habit habit) {
        if (habit == null) {
            throw new IllegalArgumentException("El hábito no puede ser nulo");
        }
        if (habit.getName() == null || habit.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre del hábito no puede estar vacío");
        }
        if (habit.getName().length() > 200) {
            throw new IllegalArgumentException("El nombre del hábito no puede superar 200 caracteres");
        }
        if (habit.getFrequency() == null) {
            throw new IllegalArgumentException("La frecuencia del hábito es obligatoria");
        }
        if (habit.getGoalDays() <= 0) {
            throw new IllegalArgumentException("La meta de días debe ser mayor a 0");
        }
        if (habit.getCategoryId() <= 0) {
            throw new IllegalArgumentException("Se requiere una categoría válida");
        }
    }
}
