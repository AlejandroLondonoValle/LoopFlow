package com.loopflow.model;

import com.loopflow.model.enums.Frequency;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad: Hábito del usuario.
 * Mapea a la tabla {@code habits} en MySQL.
 */
public class Habit {

    private int id;
    private int categoryId;
    private String name;
    private String description;
    private Frequency frequency;
    private int goalDays;
    private LocalDate startDate;
    private boolean isActive;
    private LocalDateTime createdAt;

    // Campo extra (JOIN) para mostrar el nombre de la categoría en respuestas de API
    private String categoryName;
    private String categoryColor;
    private String categoryIcon;

    // --- Constructores ---

    public Habit() {}

    public Habit(int id, int categoryId, String name, String description,
                 Frequency frequency, int goalDays, LocalDate startDate,
                 boolean isActive, LocalDateTime createdAt) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.frequency = frequency;
        this.goalDays = goalDays;
        this.startDate = startDate;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    /** Constructor para crear un nuevo hábito (sin id ni timestamp). */
    public Habit(int categoryId, String name, String description,
                 Frequency frequency, int goalDays, LocalDate startDate) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.frequency = frequency;
        this.goalDays = goalDays;
        this.startDate = startDate;
        this.isActive = true;
    }

    // --- Getters y Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Frequency getFrequency() { return frequency; }
    public void setFrequency(Frequency frequency) { this.frequency = frequency; }

    public int getGoalDays() { return goalDays; }
    public void setGoalDays(int goalDays) { this.goalDays = goalDays; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Campos de JOIN (no persistidos directamente, para respuestas enriquecidas)
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryColor() { return categoryColor; }
    public void setCategoryColor(String categoryColor) { this.categoryColor = categoryColor; }

    public String getCategoryIcon() { return categoryIcon; }
    public void setCategoryIcon(String categoryIcon) { this.categoryIcon = categoryIcon; }

    // --- equals / hashCode / toString ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Habit h)) return false;
        return id == h.id && Objects.equals(name, h.name);
    }

    @Override
    public int hashCode() { return Objects.hash(id, name); }

    @Override
    public String toString() {
        return "Habit{id=" + id + ", name='" + name + "', frequency=" + frequency + ", active=" + isActive + "}";
    }
}
