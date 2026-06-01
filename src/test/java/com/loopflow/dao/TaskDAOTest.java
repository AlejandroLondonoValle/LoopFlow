package com.loopflow.dao;

import com.loopflow.dao.impl.TaskDAOImpl;
import com.loopflow.model.Task;
import com.loopflow.model.enums.Priority;
import com.loopflow.model.enums.TaskStatus;
import org.junit.jupiter.api.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para {@link TaskDAOImpl} usando H2 en-memoria.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskDAOTest {

    private static Connection connection;
    private static TaskDAOImpl dao;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        connection = DriverManager.getConnection(
                "jdbc:h2:mem:testdb_task;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=VALUE",
                "sa", "");

        InputStream is = TaskDAOTest.class.getResourceAsStream("/test-schema.sql");
        String schema = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        try (Statement stmt = connection.createStatement()) {
            for (String sql : schema.split(";")) {
                String t = sql.trim();
                if (!t.isEmpty()) stmt.execute(t);
            }
        }
        dao = new TaskDAOImpl(connection);
    }

    @BeforeEach
    void cleanTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM tasks");
        }
    }

    @AfterAll
    static void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) connection.close();
    }

    @Test
    @Order(1)
    @DisplayName("save() persiste una tarea con ID generado")
    void testSave() {
        Task task = new Task("Diseñar UI", "Crear mockup", TaskStatus.TODO, Priority.HIGH, LocalDate.now().plusDays(3));
        Task saved = dao.save(task);

        assertNotNull(saved);
        assertTrue(saved.getId() > 0);
        assertEquals("Diseñar UI", saved.getTitle());
        assertEquals(TaskStatus.TODO, saved.getStatus());
        assertEquals(Priority.HIGH, saved.getPriority());
    }

    @Test
    @Order(2)
    @DisplayName("findAll() retorna todas las tareas")
    void testFindAll() {
        dao.save(new Task("Tarea 1", null, TaskStatus.TODO, Priority.LOW, null));
        dao.save(new Task("Tarea 2", null, TaskStatus.IN_PROGRESS, Priority.HIGH, null));

        List<Task> all = dao.findAll();
        assertEquals(2, all.size());
    }

    @Test
    @Order(3)
    @DisplayName("findByStatus() filtra por estado correctamente")
    void testFindByStatus() {
        dao.save(new Task("Todo 1", null, TaskStatus.TODO, Priority.MEDIUM, null));
        dao.save(new Task("Todo 2", null, TaskStatus.TODO, Priority.LOW, null));
        dao.save(new Task("Done 1", null, TaskStatus.DONE, Priority.HIGH, null));

        List<Task> todos = dao.findByStatus(TaskStatus.TODO);
        assertEquals(2, todos.size());
        assertTrue(todos.stream().allMatch(t -> t.getStatus() == TaskStatus.TODO));

        List<Task> done = dao.findByStatus(TaskStatus.DONE);
        assertEquals(1, done.size());
    }

    @Test
    @Order(4)
    @DisplayName("updateStatus() cambia el estado de la tarea")
    void testUpdateStatus() {
        Task task = dao.save(new Task("Mover", null, TaskStatus.TODO, Priority.MEDIUM, null));

        boolean updated = dao.updateStatus(task.getId(), TaskStatus.IN_PROGRESS);
        assertTrue(updated);

        Optional<Task> found = dao.findById(task.getId());
        assertTrue(found.isPresent());
        assertEquals(TaskStatus.IN_PROGRESS, found.get().getStatus());
    }

    @Test
    @Order(5)
    @DisplayName("update() modifica todos los campos de una tarea")
    void testUpdate() {
        Task task = dao.save(new Task("Original", "desc", TaskStatus.TODO, Priority.LOW, null));
        task.setTitle("Actualizada");
        task.setPriority(Priority.CRITICAL);
        task.setStatus(TaskStatus.DONE);

        boolean updated = dao.update(task);
        assertTrue(updated);

        Optional<Task> found = dao.findById(task.getId());
        assertTrue(found.isPresent());
        assertEquals("Actualizada", found.get().getTitle());
        assertEquals(Priority.CRITICAL, found.get().getPriority());
        assertEquals(TaskStatus.DONE, found.get().getStatus());
    }

    @Test
    @Order(6)
    @DisplayName("delete() elimina la tarea correctamente")
    void testDelete() {
        Task task = dao.save(new Task("Borrar", null, TaskStatus.TODO, Priority.LOW, null));
        boolean deleted = dao.delete(task.getId());

        assertTrue(deleted);
        assertTrue(dao.findById(task.getId()).isEmpty());
    }

    @Test
    @Order(7)
    @DisplayName("findByPriority() filtra por prioridad correctamente")
    void testFindByPriority() {
        dao.save(new Task("Crítica 1", null, TaskStatus.TODO, Priority.CRITICAL, null));
        dao.save(new Task("Crítica 2", null, TaskStatus.IN_PROGRESS, Priority.CRITICAL, null));
        dao.save(new Task("Baja", null, TaskStatus.TODO, Priority.LOW, null));

        List<Task> critical = dao.findByPriority(Priority.CRITICAL);
        assertEquals(2, critical.size());
    }
}
