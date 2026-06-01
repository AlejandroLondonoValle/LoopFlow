package com.loopflow.service;

import com.loopflow.dao.DailyLogDAO;
import com.loopflow.dao.HabitDAO;
import com.loopflow.model.DailyLog;
import com.loopflow.model.Habit;
import com.loopflow.model.enums.Frequency;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link HabitService} usando Mockito.
 * Verifica la lógica de negocio sin acceder a la base de datos.
 */
@ExtendWith(MockitoExtension.class)
class HabitServiceTest {

    @Mock private HabitDAO habitDAO;
    @Mock private DailyLogDAO dailyLogDAO;

    private HabitService service;

    @BeforeEach
    void setUp() {
        service = new HabitService(habitDAO, dailyLogDAO);
    }

    // ===========================
    // Tests de CRUD
    // ===========================

    @Test
    @DisplayName("createHabit() guarda un hábito válido")
    void testCreateHabit_valid() {
        Habit habit = new Habit(1, "Meditación", null, Frequency.DAILY, 30, LocalDate.now());
        when(habitDAO.save(any())).thenReturn(habit);

        Habit result = service.createHabit(habit);

        assertNotNull(result);
        verify(habitDAO, times(1)).save(any(Habit.class));
    }

    @Test
    @DisplayName("createHabit() lanza excepción con nombre vacío")
    void testCreateHabit_emptyName() {
        Habit habit = new Habit(1, "", null, Frequency.DAILY, 30, LocalDate.now());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createHabit(habit));
        assertTrue(ex.getMessage().contains("nombre"));
        verify(habitDAO, never()).save(any());
    }

    @Test
    @DisplayName("createHabit() lanza excepción con goalDays <= 0")
    void testCreateHabit_invalidGoalDays() {
        Habit habit = new Habit(1, "Test", null, Frequency.DAILY, 0, LocalDate.now());

        assertThrows(IllegalArgumentException.class, () -> service.createHabit(habit));
    }

    @Test
    @DisplayName("getHabitById() lanza excepción para ID inexistente")
    void testGetHabitById_notFound() {
        when(habitDAO.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getHabitById(999));
    }

    // ===========================
    // Tests de calculateStreak
    // ===========================

    @Test
    @DisplayName("calculateStreak() retorna 0 cuando no hay logs")
    void testCalculateStreak_noLogs() {
        LocalDate start = LocalDate.now().minusDays(10);
        Habit habit = buildHabit(1, start);
        when(habitDAO.findById(1)).thenReturn(Optional.of(habit));
        when(dailyLogDAO.findByHabitAndDateRange(eq(1), any(), any()))
                .thenReturn(Collections.emptyList());

        int streak = service.calculateStreak(1);
        assertEquals(0, streak);
    }

    @Test
    @DisplayName("calculateStreak() cuenta días consecutivos correctamente")
    void testCalculateStreak_threeDays() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(30);
        Habit habit = buildHabit(1, start);

        List<DailyLog> logs = Arrays.asList(
                buildLog(1, today, true),
                buildLog(1, today.minusDays(1), true),
                buildLog(1, today.minusDays(2), true),
                buildLog(1, today.minusDays(3), false) // Rompe la racha
        );

        when(habitDAO.findById(1)).thenReturn(Optional.of(habit));
        when(dailyLogDAO.findByHabitAndDateRange(eq(1), any(), any())).thenReturn(logs);

        int streak = service.calculateStreak(1);
        assertEquals(3, streak);
    }

    @Test
    @DisplayName("calculateStreak() retorna 0 si hoy no está completado y ayer tampoco")
    void testCalculateStreak_notCompletedToday() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(5);
        Habit habit = buildHabit(1, start);

        List<DailyLog> logs = List.of(
                buildLog(1, today.minusDays(3), true),
                buildLog(1, today.minusDays(4), true)
        );

        when(habitDAO.findById(1)).thenReturn(Optional.of(habit));
        when(dailyLogDAO.findByHabitAndDateRange(eq(1), any(), any())).thenReturn(logs);

        int streak = service.calculateStreak(1);
        assertEquals(0, streak);
    }

    // ===========================
    // Tests de getCompletionRate
    // ===========================

    @Test
    @DisplayName("getCompletionRate() calcula tasa correctamente — 2 de 4 días")
    void testGetCompletionRate_halfCompleted() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(3);
        Habit habit = buildHabit(1, start);

        List<DailyLog> logs = Arrays.asList(
                buildLog(1, today, true),
                buildLog(1, today.minusDays(1), false),
                buildLog(1, today.minusDays(2), true),
                buildLog(1, today.minusDays(3), false)
        );

        when(habitDAO.findById(1)).thenReturn(Optional.of(habit));
        when(dailyLogDAO.findByHabitAndDateRange(eq(1), any(), any())).thenReturn(logs);

        double rate = service.getCompletionRate(1, start, today);
        assertEquals(50.0, rate, 0.01);
    }

    @Test
    @DisplayName("getCompletionRate() retorna 100.0 cuando todos los días están completados")
    void testGetCompletionRate_fullCompletion() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(1);
        Habit habit = buildHabit(1, start);

        List<DailyLog> logs = Arrays.asList(
                buildLog(1, today, true),
                buildLog(1, start, true)
        );

        when(habitDAO.findById(1)).thenReturn(Optional.of(habit));
        when(dailyLogDAO.findByHabitAndDateRange(eq(1), any(), any())).thenReturn(logs);

        double rate = service.getCompletionRate(1, start, today);
        assertEquals(100.0, rate, 0.01);
    }

    @Test
    @DisplayName("getCompletionRate() lanza excepción si from > to")
    void testGetCompletionRate_invalidRange() {
        Habit habit = buildHabit(1, LocalDate.now().minusDays(10));
        when(habitDAO.findById(1)).thenReturn(Optional.of(habit));

        LocalDate from = LocalDate.now();
        LocalDate to = LocalDate.now().minusDays(1);

        assertThrows(IllegalArgumentException.class, () -> service.getCompletionRate(1, from, to));
    }

    // ===========================
    // Helpers
    // ===========================

    private Habit buildHabit(int id, LocalDate start) {
        Habit h = new Habit();
        h.setId(id);
        h.setName("Test Habit");
        h.setFrequency(Frequency.DAILY);
        h.setGoalDays(30);
        h.setStartDate(start);
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
}
