package com.loopflow.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad: Categoría de hábitos.
 * Mapea a la tabla {@code categories} en MySQL.
 */
public class Category {

    private int id;
    private String name;
    private String color;
    private String icon;
    private LocalDateTime createdAt;

    // --- Constructores ---

    public Category() {}

    public Category(int id, String name, String color, String icon, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.icon = icon;
        this.createdAt = createdAt;
    }

    /** Constructor para crear una nueva categoría (sin id ni timestamp). */
    public Category(String name, String color, String icon) {
        this.name = name;
        this.color = color;
        this.icon = icon;
    }

    // --- Getters y Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // --- equals / hashCode / toString ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category c)) return false;
        return id == c.id && Objects.equals(name, c.name);
    }

    @Override
    public int hashCode() { return Objects.hash(id, name); }

    @Override
    public String toString() {
        return "Category{id=" + id + ", name='" + name + "', color='" + color + "'}";
    }
}
