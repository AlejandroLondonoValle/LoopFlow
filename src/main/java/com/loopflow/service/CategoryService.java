package com.loopflow.service;

import com.loopflow.dao.CategoryDAO;
import com.loopflow.model.Category;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de lógica de negocio para {@link Category}.
 * Delega la persistencia al {@link CategoryDAO} inyectado.
 */
public class CategoryService {

    private final CategoryDAO categoryDAO;

    public CategoryService(CategoryDAO categoryDAO) {
        this.categoryDAO = categoryDAO;
    }

    /** Retorna todas las categorías. */
    public List<Category> getAllCategories() {
        return categoryDAO.findAll();
    }

    /**
     * Busca una categoría por ID.
     *
     * @throws IllegalArgumentException si no existe la categoría
     */
    public Category getCategoryById(int id) {
        return categoryDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada con id=" + id));
    }

    /**
     * Crea una nueva categoría.
     * Valida que el nombre no esté vacío y que no exista otra con el mismo nombre.
     *
     * @param category datos de la categoría a crear
     * @return la categoría creada con su ID asignado
     */
    public Category createCategory(Category category) {
        validateCategory(category);
        // Verificar nombre duplicado
        categoryDAO.findByName(category.getName()).ifPresent(existing -> {
            throw new IllegalArgumentException("Ya existe una categoría con el nombre: " + category.getName());
        });
        return categoryDAO.save(category);
    }

    /**
     * Actualiza una categoría existente.
     *
     * @param id       ID de la categoría a actualizar
     * @param category nuevos datos
     * @return la categoría actualizada
     */
    public Category updateCategory(int id, Category category) {
        validateCategory(category);
        // Verificar que existe
        getCategoryById(id);
        // Verificar nombre duplicado en otra categoría
        categoryDAO.findByName(category.getName()).ifPresent(existing -> {
            if (existing.getId() != id) {
                throw new IllegalArgumentException("Ya existe otra categoría con el nombre: " + category.getName());
            }
        });
        category.setId(id);
        if (!categoryDAO.update(category)) {
            throw new RuntimeException("No se pudo actualizar la categoría id=" + id);
        }
        return getCategoryById(id);
    }

    /**
     * Elimina una categoría por ID.
     * Los hábitos asociados se eliminan en cascada (ON DELETE CASCADE en BD).
     *
     * @param id ID de la categoría
     * @throws IllegalArgumentException si no existe
     */
    public void deleteCategory(int id) {
        getCategoryById(id); // Verifica existencia
        if (!categoryDAO.delete(id)) {
            throw new RuntimeException("No se pudo eliminar la categoría id=" + id);
        }
    }

    // --- Validaciones ---

    private void validateCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("La categoría no puede ser nula");
        }
        if (category.getName() == null || category.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría no puede estar vacío");
        }
        if (category.getName().length() > 100) {
            throw new IllegalArgumentException("El nombre de la categoría no puede superar 100 caracteres");
        }
        if (category.getColor() != null && !category.getColor().matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("El color debe ser un código hexadecimal válido (ej: #2F49A1)");
        }
    }
}
