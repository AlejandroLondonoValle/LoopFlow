package com.loopflow.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad: Registro diario de cumplimiento de un hábito.
 * Mapea a la tabla {@code daily_logs} en MySQL.
 * La combinación (habit_id, log_date) es UNIQUE — un solo registro por día por hábito.
 */
public class DailyLog {

    private int id;
    private int habitId;
    private LocalDate logDate;
    private boolean completed;
    private String notes;
    private LocalDateTime createdAt;

    // --- Constructores ---

    public DailyLog() {}

    public DailyLog(int id, int habitId, LocalDate logDate, boolean completed,
                    String notes, LocalDateTime createdAt) {
        this.id = id;
        this.habitId = habitId;
        this.logDate = logDate;
        this.completed = completed;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    /** Constructor para crear un nuevo registro (sin id ni timestamp). */
    public DailyLog(int habitId, LocalDate logDate, boolean completed, String notes) {
        this.habitId = habitId;
        this.logDate = logDate;
        this.completed = completed;
        this.notes = notes;
    }

    // --- Getters y Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getHabitId() { return habitId; }
    public void setHabitId(int habitId) { this.habitId = habitId; }

    public LocalDate getLogDate() { return logDate; }
    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // --- equals / hashCode / toString ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyLog d)) return false;
        return id == d.id && habitId == d.habitId && Objects.equals(logDate, d.logDate);
    }

    @Override
    public int hashCode() { return Objects.hash(id, habitId, logDate); }

    @Override
    public String toString() {
        return "DailyLog{id=" + id + ", habitId=" + habitId +
               ", date=" + logDate + ", completed=" + completed + "}";
    }
}
