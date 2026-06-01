package com.loopflow.dao;

import com.loopflow.model.Category;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la entidad {@link Category}.
 */
public interface CategoryDAO {

    /** Retorna todas las categorías ordenadas por nombre. */
    List<Category> findAll();

    /** Busca una categoría por su ID. */
    Optional<Category> findById(int id);

    /** Busca una categoría por su nombre exacto. */
    Optional<Category> findByName(String name);

    /**
     * Persiste una nueva categoría en la base de datos.
     *
     * @param category entidad a guardar (sin id)
     * @return la categoría con el id generado
     */
    Category save(Category category);

    /**
     * Actualiza una categoría existente.
     *
     * @param category entidad con datos actualizados (debe tener id válido)
     * @return true si se actualizó al menos una fila
     */
    boolean update(Category category);

    /**
     * Elimina una categoría por su ID.
     * Los hábitos asociados se eliminan en cascada (ON DELETE CASCADE en BD).
     *
     * @param id ID de la categoría
     * @return true si se eliminó al menos una fila
     */
    boolean delete(int id);
}
