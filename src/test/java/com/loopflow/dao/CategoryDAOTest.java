package com.loopflow.dao;

import com.loopflow.dao.impl.CategoryDAOImpl;
import com.loopflow.model.Category;
import org.junit.jupiter.api.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para {@link CategoryDAOImpl} usando H2 en-memoria.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CategoryDAOTest {

    private static Connection connection;
    private static CategoryDAOImpl dao;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        // H2 en modo MySQL para máxima compatibilidad
        connection = DriverManager.getConnection(
                "jdbc:h2:mem:testdb_category;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=VALUE",
                "sa", "");

        // Cargar el esquema de test
        InputStream is = CategoryDAOTest.class.getResourceAsStream("/test-schema.sql");
        assertNotNull(is, "test-schema.sql no encontrado en classpath");
        String schema = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        // Ejecutar las sentencias del esquema
        try (Statement stmt = connection.createStatement()) {
            for (String sql : schema.split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) stmt.execute(trimmed);
            }
        }
        dao = new CategoryDAOImpl(connection);
    }

    @BeforeEach
    void cleanTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM categories");
            stmt.execute("ALTER TABLE categories ALTER COLUMN id RESTART WITH 1");
        }
    }

    @AfterAll
    static void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("save() persiste una categoría y retorna con ID generado")
    void testSave() {
        Category cat = new Category("Salud", "#22C55E", "heart");
        Category saved = dao.save(cat);

        assertNotNull(saved);
        assertTrue(saved.getId() > 0);
        assertEquals("Salud", saved.getName());
        assertEquals("#22C55E", saved.getColor());
    }

    @Test
    @Order(2)
    @DisplayName("findAll() retorna lista correcta")
    void testFindAll() {
        dao.save(new Category("Salud", "#22C55E", "heart"));
        dao.save(new Category("Ejercicio", "#EF4444", "activity"));

        List<Category> all = dao.findAll();
        assertEquals(2, all.size());
    }

    @Test
    @Order(3)
    @DisplayName("findById() encuentra categoría existente")
    void testFindById() {
        Category saved = dao.save(new Category("Aprendizaje", "#F59E0B", "book"));
        Optional<Category> found = dao.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Aprendizaje", found.get().getName());
    }

    @Test
    @Order(4)
    @DisplayName("findById() retorna empty para ID inexistente")
    void testFindByIdNotFound() {
        Optional<Category> found = dao.findById(9999);
        assertTrue(found.isEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("findByName() encuentra categoría por nombre exacto")
    void testFindByName() {
        dao.save(new Category("Bienestar", "#8B5CF6", "star"));
        Optional<Category> found = dao.findByName("Bienestar");

        assertTrue(found.isPresent());
        assertEquals("#8B5CF6", found.get().getColor());
    }

    @Test
    @Order(6)
    @DisplayName("update() modifica los datos correctamente")
    void testUpdate() {
        Category saved = dao.save(new Category("Old Name", "#000000", "x"));
        saved.setName("New Name");
        saved.setColor("#FFFFFF");

        boolean updated = dao.update(saved);
        assertTrue(updated);

        Optional<Category> found = dao.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("New Name", found.get().getName());
        assertEquals("#FFFFFF", found.get().getColor());
    }

    @Test
    @Order(7)
    @DisplayName("delete() elimina la categoría y retorna true")
    void testDelete() {
        Category saved = dao.save(new Category("ToDelete", "#123456", "trash"));
        boolean deleted = dao.delete(saved.getId());

        assertTrue(deleted);
        assertTrue(dao.findById(saved.getId()).isEmpty());
    }

    @Test
    @Order(8)
    @DisplayName("delete() retorna false para ID inexistente")
    void testDeleteNotFound() {
        boolean deleted = dao.delete(9999);
        assertFalse(deleted);
    }
}
