package com.loopflow.service;

import com.loopflow.dao.DailyLogDAO;
import com.loopflow.dao.HabitDAO;
import com.loopflow.dao.TaskDAO;
import com.loopflow.model.DailyLog;
import com.loopflow.model.Habit;
import com.loopflow.model.Task;
import com.loopflow.model.enums.TaskStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio que agrega datos de hábitos y tareas para el Dashboard principal.
 * Proporciona una vista unificada del día actual.
 */
public class DashboardService {

    private final HabitDAO habitDAO;
    private final DailyLogDAO dailyLogDAO;
    private final TaskDAO taskDAO;

    public DashboardService(HabitDAO habitDAO, DailyLogDAO dailyLogDAO, TaskDAO taskDAO) {
        this.habitDAO = habitDAO;
        this.dailyLogDAO = dailyLogDAO;
        this.taskDAO = taskDAO;
    }

    /**
     * Retorna el resumen del dashboard para el día actual.
     *
     * @return {@link DashboardSummary} con hábitos del día y tareas por columna
     */
    public DashboardSummary getDashboardSummary() {
        LocalDate today = LocalDate.now();

        // --- Hábitos activos ---
        List<Habit> activeHabits = habitDAO.findActive();

        // Registros del día de hoy
        List<DailyLog> todayLogs = dailyLogDAO.findByDate(today);
        Map<Integer, Boolean> completionMap = todayLogs.stream()
                .collect(Collectors.toMap(DailyLog::getHabitId, DailyLog::isCompleted));

        // Enriquecer hábitos con estado del día
        List<HabitDaySummary> habitSummaries = activeHabits.stream()
                .map(h -> new HabitDaySummary(
                        h,
                        Boolean.TRUE.equals(completionMap.get(h.getId()))
                ))
                .collect(Collectors.toList());

        long completedToday = habitSummaries.stream().filter(HabitDaySummary::isCompletedToday).count();
        long pendingToday = habitSummaries.size() - completedToday;

        // --- Tareas por columna ---
        List<Task> todoTasks       = taskDAO.findByStatus(TaskStatus.TODO);
        List<Task> inProgressTasks = taskDAO.findByStatus(TaskStatus.IN_PROGRESS);
        List<Task> doneTasks       = taskDAO.findByStatus(TaskStatus.DONE);

        return new DashboardSummary(
                today,
                habitSummaries,
                (int) completedToday,
                (int) pendingToday,
                todoTasks,
                inProgressTasks,
                doneTasks
        );
    }

    // ===========================
    // Inner Classes (DTOs)
    // ===========================

    /**
     * DTO de resumen del dashboard.
     */
    public static class DashboardSummary {
        private final LocalDate date;
        private final List<HabitDaySummary> habits;
        private final int habitsCompletedToday;
        private final int habitsPendingToday;
        private final List<Task> todoTasks;
        private final List<Task> inProgressTasks;
        private final List<Task> doneTasks;

        public DashboardSummary(LocalDate date, List<HabitDaySummary> habits,
                                int habitsCompletedToday, int habitsPendingToday,
                                List<Task> todoTasks, List<Task> inProgressTasks,
                                List<Task> doneTasks) {
            this.date = date;
            this.habits = habits;
            this.habitsCompletedToday = habitsCompletedToday;
            this.habitsPendingToday = habitsPendingToday;
            this.todoTasks = todoTasks;
            this.inProgressTasks = inProgressTasks;
            this.doneTasks = doneTasks;
        }

        public LocalDate getDate() { return date; }
        public List<HabitDaySummary> getHabits() { return habits; }
        public int getHabitsCompletedToday() { return habitsCompletedToday; }
        public int getHabitsPendingToday() { return habitsPendingToday; }
        public int getTotalHabits() { return habits.size(); }
        public List<Task> getTodoTasks() { return todoTasks; }
        public List<Task> getInProgressTasks() { return inProgressTasks; }
        public List<Task> getDoneTasks() { return doneTasks; }
    }

    /**
     * DTO que une un hábito con su estado de completado del día actual.
     */
    public static class HabitDaySummary {
        private final Habit habit;
        private final boolean completedToday;

        public HabitDaySummary(Habit habit, boolean completedToday) {
            this.habit = habit;
            this.completedToday = completedToday;
        }

        public Habit getHabit() { return habit; }
        public boolean isCompletedToday() { return completedToday; }
    }
}
