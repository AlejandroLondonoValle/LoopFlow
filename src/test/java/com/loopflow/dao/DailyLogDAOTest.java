package com.loopflow.dao;

import com.loopflow.dao.impl.DailyLogDAOImpl;
import com.loopflow.dao.impl.HabitDAOImpl;
import com.loopflow.model.DailyLog;
import com.loopflow.model.Habit;
import com.loopflow.model.enums.Frequency;
import org.junit.jupiter.api.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para {@link DailyLogDAOImpl} usando H2 en-memoria.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DailyLogDAOTest {

    private static Connection connection;
    private static DailyLogDAOImpl dao;
    private static int testHabitId;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        connection = DriverManager.getConnection(
                "jdbc:h2:mem:testdb_dailylog;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=VALUE",
                "sa", "");

        InputStream is = DailyLogDAOTest.class.getResourceAsStream("/test-schema.sql");
        String schema = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        try (Statement stmt = connection.createStatement()) {
            for (String sql : schema.split(";")) {
                String t = sql.trim();
                if (!t.isEmpty()) stmt.execute(t);
            }
        }

        // Insertar datos de setup: categoría y hábito
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO categories (name, color, icon) VALUES ('Test', '#000', 'x')");
        }
        HabitDAOImpl habitDAO = new HabitDAOImpl(connection);
        Habit habit = new Habit(1, "Test Habit", null, Frequency.DAILY, 30, LocalDate.now().minusDays(10));
        Habit saved = habitDAO.save(habit);
        testHabitId = saved.getId();

        dao = new DailyLogDAOImpl(connection);
    }

    @BeforeEach
    void cleanLogs() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM daily_logs");
        }
    }

    @AfterAll
    static void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) connection.close();
    }

    @Test
    @Order(1)
    @DisplayName("save() persiste un registro diario")
    void testSave() {
        DailyLog log = new DailyLog(testHabitId, LocalDate.now(), true, "Bien hecho");
        DailyLog saved = dao.save(log);

        assertNotNull(saved);
        assertTrue(saved.getId() > 0);
        assertTrue(saved.isCompleted());
    }

    @Test
    @Order(2)
    @DisplayName("findByHabitAndDate() encuentra el registro correcto")
    void testFindByHabitAndDate() {
        LocalDate today = LocalDate.now();
        dao.save(new DailyLog(testHabitId, today, true, "Test"));

        Optional<DailyLog> found = dao.findByHabitAndDate(testHabitId, today);
        assertTrue(found.isPresent());
        assertEquals(testHabitId, found.get().getHabitId());
        assertTrue(found.get().isCompleted());
    }

    @Test
    @Order(3)
    @DisplayName("markCompleted() realiza UPSERT correctamente")
    void testMarkCompleted() {
        LocalDate today = LocalDate.now();

        // Primera llamada: INSERT
        DailyLog first = dao.markCompleted(testHabitId, today, false, "Primera vez");
        assertFalse(first.isCompleted());

        // Segunda llamada: UPDATE
        DailyLog second = dao.markCompleted(testHabitId, today, true, "Actualizado");
        assertTrue(second.isCompleted());
        assertEquals("Actualizado", second.getNotes());

        // Verificar que solo hay un registro
        List<DailyLog> logs = dao.findByHabit(testHabitId);
        assertEquals(1, logs.size());
    }

    @Test
    @Order(4)
    @DisplayName("findByHabitAndDateRange() retorna logs en rango correcto")
    void testFindByHabitAndDateRange() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate twoDaysAgo = today.minusDays(2);

        dao.save(new DailyLog(testHabitId, today, true, null));
        dao.save(new DailyLog(testHabitId, yesterday, true, null));
        dao.save(new DailyLog(testHabitId, twoDaysAgo, false, null));

        List<DailyLog> range = dao.findByHabitAndDateRange(testHabitId, twoDaysAgo, today);
        assertEquals(3, range.size());

        // Solo los del rango yesterday-today
        List<DailyLog> partial = dao.findByHabitAndDateRange(testHabitId, yesterday, today);
        assertEquals(2, partial.size());
    }

    @Test
    @Order(5)
    @DisplayName("findByDate() retorna todos los registros de una fecha")
    void testFindByDate() {
        LocalDate today = LocalDate.now();
        dao.save(new DailyLog(testHabitId, today, true, null));

        List<DailyLog> logs = dao.findByDate(today);
        assertFalse(logs.isEmpty());
        assertTrue(logs.stream().allMatch(l -> l.getLogDate().equals(today)));
    }

    @Test
    @Order(6)
    @DisplayName("delete() elimina un registro existente")
    void testDelete() {
        DailyLog saved = dao.save(new DailyLog(testHabitId, LocalDate.now().minusDays(5), true, null));
        boolean deleted = dao.delete(saved.getId());
        assertTrue(deleted);
        assertTrue(dao.findByHabitAndDate(testHabitId, LocalDate.now().minusDays(5)).isEmpty());
    }
}
