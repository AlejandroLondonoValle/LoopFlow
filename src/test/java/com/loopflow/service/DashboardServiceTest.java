package com.loopflow.service;

import com.loopflow.dao.DailyLogDAO;
import com.loopflow.dao.HabitDAO;
import com.loopflow.dao.TaskDAO;
import com.loopflow.model.DailyLog;
import com.loopflow.model.Habit;
import com.loopflow.model.Task;
import com.loopflow.model.enums.Frequency;
import com.loopflow.model.enums.Priority;
import com.loopflow.model.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link DashboardService} usando Mockito.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private HabitDAO habitDAO;
    @Mock private DailyLogDAO dailyLogDAO;
    @Mock private TaskDAO taskDAO;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(habitDAO, dailyLogDAO, taskDAO);
    }

    @Test
    @DisplayName("getDashboardSummary() retorna hábitos y tareas correctamente")
    void testGetDashboardSummary_basicData() {
        LocalDate today = LocalDate.now();

        // Setup hábitos activos
        Habit habit1 = buildHabit(1, "Meditación");
        Habit habit2 = buildHabit(2, "Ejercicio");
        when(habitDAO.findActive()).thenReturn(Arrays.asList(habit1, habit2));

        // Setup logs del día — solo habit1 completado
        DailyLog log1 = buildLog(1, today, true);
        when(dailyLogDAO.findByDate(today)).thenReturn(List.of(log1));

        // Setup tareas por columna
        Task todo1 = buildTask(1, "Tarea A", TaskStatus.TODO);
        Task inProg1 = buildTask(2, "Tarea B", TaskStatus.IN_PROGRESS);
        Task done1 = buildTask(3, "Tarea C", TaskStatus.DONE);

        when(taskDAO.findByStatus(TaskStatus.TODO)).thenReturn(List.of(todo1));
        when(taskDAO.findByStatus(TaskStatus.IN_PROGRESS)).thenReturn(List.of(inProg1));
        when(taskDAO.findByStatus(TaskStatus.DONE)).thenReturn(List.of(done1));

        DashboardService.DashboardSummary summary = service.getDashboardSummary();

        assertNotNull(summary);
        assertEquals(today, summary.getDate());

        // Hábitos
        assertEquals(2, summary.getTotalHabits());
        assertEquals(1, summary.getHabitsCompletedToday());
        assertEquals(1, summary.getHabitsPendingToday());

        // Verificar estado individual
        assertTrue(summary.getHabits().stream()
                .filter(h -> h.getHabit().getId() == 1)
                .findFirst()
                .map(DashboardService.HabitDaySummary::isCompletedToday)
                .orElse(false));

        assertFalse(summary.getHabits().stream()
                .filter(h -> h.getHabit().getId() == 2)
                .findFirst()
                .map(DashboardService.HabitDaySummary::isCompletedToday)
                .orElse(true));

        // Tareas
        assertEquals(1, summary.getTodoTasks().size());
        assertEquals(1, summary.getInProgressTasks().size());
        assertEquals(1, summary.getDoneTasks().size());
    }

    @Test
    @DisplayName("getDashboardSummary() funciona cuando no hay hábitos ni tareas")
    void testGetDashboardSummary_empty() {
        when(habitDAO.findActive()).thenReturn(Collections.emptyList());
        when(dailyLogDAO.findByDate(any())).thenReturn(Collections.emptyList());
        when(taskDAO.findByStatus(any())).thenReturn(Collections.emptyList());

        DashboardService.DashboardSummary summary = service.getDashboardSummary();

        assertNotNull(summary);
        assertEquals(0, summary.getTotalHabits());
        assertEquals(0, summary.getHabitsCompletedToday());
        assertEquals(0, summary.getHabitsPendingToday());
        assertTrue(summary.getTodoTasks().isEmpty());
        assertTrue(summary.getInProgressTasks().isEmpty());
        assertTrue(summary.getDoneTasks().isEmpty());
    }

    @Test
    @DisplayName("getDashboardSummary() marca todos los hábitos como completados")
    void testGetDashboardSummary_allCompleted() {
        LocalDate today = LocalDate.now();
        Habit habit1 = buildHabit(1, "Habit A");
        Habit habit2 = buildHabit(2, "Habit B");
        when(habitDAO.findActive()).thenReturn(Arrays.asList(habit1, habit2));
        when(dailyLogDAO.findByDate(today)).thenReturn(Arrays.asList(
                buildLog(1, today, true),
                buildLog(2, today, true)
        ));
        when(taskDAO.findByStatus(any())).thenReturn(Collections.emptyList());

        DashboardService.DashboardSummary summary = service.getDashboardSummary();

        assertEquals(2, summary.getHabitsCompletedToday());
        assertEquals(0, summary.getHabitsPendingToday());
        assertTrue(summary.getHabits().stream().allMatch(DashboardService.HabitDaySummary::isCompletedToday));
    }

    // ===========================
    // Helpers
    // ===========================

    private Habit buildHabit(int id, String name) {
        Habit h = new Habit();
        h.setId(id);
        h.setName(name);
        h.setFrequency(Frequency.DAILY);
        h.setGoalDays(30);
        h.setStartDate(LocalDate.now().minusDays(10));
        h.setActive(true);
        h.setCategoryId(1);
        return h;
    }

    private DailyLog buildLog(int habitId, LocalDate date, boolean completed) {
        DailyLog log = new DailyLog();
        log.setHabitId(habitId);
        log.setLogDate(date);
        log.setCompleted(completed);
        return log;
    }

    private Task buildTask(int id, String title, TaskStatus status) {
        Task t = new Task();
        t.setId(id);
        t.setTitle(title);
        t.setStatus(status);
        t.setPriority(Priority.MEDIUM);
        return t;
    }
}
