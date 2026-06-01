package com.loopflow.dao;

import com.loopflow.model.Habit;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la entidad {@link Habit}.
 */
public interface HabitDAO {

    /** Retorna todos los hábitos (activos e inactivos) con datos de categoría (JOIN). */
    List<Habit> findAll();

    /** Retorna solo los hábitos activos con datos de categoría (JOIN). */
    List<Habit> findActive();

    /** Retorna todos los hábitos de una categoría específica. */
    List<Habit> findByCategory(int categoryId);

    /** Busca un hábito por su ID (incluye datos de categoría). */
    Optional<Habit> findById(int id);

    /**
     * Persiste un nuevo hábito en la base de datos.
     *
     * @param habit entidad a guardar (sin id)
     * @return el hábito con el id generado
     */
    Habit save(Habit habit);

    /**
     * Actualiza un hábito existente.
     *
     * @param habit entidad con datos actualizados (debe tener id válido)
     * @return true si se actualizó al menos una fila
     */
    boolean update(Habit habit);

    /**
     * Elimina un hábito por su ID.
     * Los registros diarios asociados se eliminan en cascada.
     *
     * @param id ID del hábito
     * @return true si se eliminó al menos una fila
     */
    boolean delete(int id);

    /**
     * Cambia el estado activo/inactivo de un hábito (archivo).
     *
     * @param id       ID del hábito
     * @param isActive true para activar, false para archivar
     * @return true si se actualizó correctamente
     */
    boolean setActive(int id, boolean isActive);
}
