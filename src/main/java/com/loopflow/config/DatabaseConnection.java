package com.loopflow.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.InputStream;
import java.util.Properties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton thread-safe para la conexión a la base de datos MySQL.
 *
 * <p>
 * Las credenciales se cargan con la siguiente prioridad:
 * <ol>
 * <li>Archivo {@code db.properties} en el classpath (resources).</li>
 * <li>Variables de entorno del sistema (ej. configuradas en Render.com / Clever
 * Cloud).</li>
 * <li>Archivo {@code .env} en el directorio raíz del proyecto (para desarrollo
 * local).</li>
 * </ol>
 *
 * <p>
 * Variables requeridas: {@code DB_URL}, {@code DB_USER}, {@code DB_PASSWORD} o equivalentes en db.properties.
 */
public class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    /** Instancia única — volatile para visibilidad entre hilos. */
    private static volatile DatabaseConnection instance;

    private Connection connection;

    /**
     * dotenv cargado en clase — ignora si el archivo .env no existe
     * (en producción se usan las variables de entorno del sistema directamente).
     */
    private static final Dotenv DOTENV = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    private final String driver;
    private final String url;
    private final String user;
    private final String password;

    // --- Constructor privado (patrón Singleton) ---

    private DatabaseConnection() {
        Properties properties = new Properties();
        String propDriver = null;
        String propUrl = null;
        String propUser = null;
        String propPassword = null;

        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                properties.load(input);
                propDriver = properties.getProperty("db.driver");
                propUrl = properties.getProperty("db.url");
                propUser = properties.getProperty("db.user");
                propPassword = properties.getProperty("db.password");
                LOGGER.info("Configuración de base de datos cargada desde db.properties.");
            }
        } catch (Exception e) {
            LOGGER.warning("No se pudo cargar db.properties: " + e.getMessage());
        }

        this.driver = propDriver != null && !propDriver.isBlank() ? propDriver : "com.mysql.cj.jdbc.Driver";
        this.url = propUrl != null && !propUrl.isBlank() ? propUrl : getEnvVar("DB_URL");
        this.user = propUser != null && !propUser.isBlank() ? propUser : getEnvVar("DB_USER");
        this.password = propPassword != null && !propPassword.isBlank() ? propPassword : getEnvVar("DB_PASSWORD");

        if (this.url == null || this.user == null || this.password == null) {
            throw new IllegalStateException(
                    "Faltan variables de configuración de base de datos. " +
                            "Asegúrate de configurar db.properties en resources o variables de entorno (DB_URL, DB_USER, DB_PASSWORD).");
        }

        connect();
    }

    // --- Singleton: doble verificación bloqueada (Double-Checked Locking) ---

    /**
     * Retorna la única instancia de {@code DatabaseConnection}.
     * Thread-safe con Double-Checked Locking.
     *
     * @return la instancia singleton
     */
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    // --- Conexión ---

    /**
     * Establece la conexión con la base de datos.
     * Registra el driver de MySQL automáticamente vía SPI (MySQL Connector/J 8+).
     */
    private void connect() {
        try {
            Class.forName(driver);
            this.connection = DriverManager.getConnection(url, user, password);
            LOGGER.info("Conexión a la base de datos establecida exitosamente.");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver no encontrado en el classpath: " + driver, e);
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo conectar a la base de datos: " + e.getMessage(), e);
        }
    }

    /**
     * Retorna la conexión activa. Si la conexión está cerrada o es nula,
     * la restablece automáticamente (reconexión lazy).
     *
     * @return la {@link Connection} activa a MySQL
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                LOGGER.warning("Conexión cerrada o nula. Reconectando...");
                connect();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al verificar el estado de la conexión. Reconectando.", e);
            connect();
        }
        return connection;
    }

    /**
     * Cierra la conexión activa. Usar al apagar la aplicación.
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    LOGGER.info("Conexión a la base de datos cerrada.");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error al cerrar la conexión.", e);
            }
        }
    }

    // --- Utilidades ---

    /**
     * Lee una variable de entorno. Primero busca en el sistema, luego en .env.
     *
     * @param key nombre de la variable
     * @return valor de la variable, o {@code null} si no existe
     */
    private static String getEnvVar(String key) {
        // 1. Variables del sistema (producción: Render / Clever Cloud)
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        // 2. Archivo .env (desarrollo local)
        return DOTENV.get(key, null);
    }

    /**
     * Permite resetear el singleton para pruebas de integración.
     * SOLO debe llamarse desde código de test.
     */
    static synchronized void resetInstance() {
        if (instance != null) {
            instance.closeConnection();
            instance = null;
        }
    }
}
