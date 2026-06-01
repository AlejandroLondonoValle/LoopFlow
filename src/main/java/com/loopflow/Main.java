package com.loopflow;

import com.loopflow.config.DatabaseConnection;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Punto de entrada de LoopFlow.
 *
 * <p>Arranca un servidor Jetty 11 embebido que sirve:
 * <ul>
 *   <li>{@code /api/*} — API REST Jersey (JAX-RS)</li>
 *   <li>{@code /*}    — Archivos estáticos del frontend (HTML/CSS/JS)</li>
 * </ul>
 *
 * <p>Puerto configurable vía variable de entorno {@code PORT} (default: 8080).
 * Render.com establece esta variable automáticamente.
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {

        // --- Puerto ---
        int port = 9000;
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                LOGGER.warning("Valor de PORT inválido: " + portEnv + ". Usando 9000.");
            }
        }

        // --- Servidor Jetty ---
        Server server = new Server(port);

        // ========================================================
        // CONTEXTO 1: API REST en /api/*
        // ========================================================
        ServletContextHandler apiContext = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        apiContext.setContextPath("/api");

        // Configuración de Jersey: escaneo automático de paquetes
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.packages(
                "com.loopflow.controller",   // Controladores REST
                "com.loopflow.filter",       // CorsFilter
                "com.loopflow.config"        // JacksonConfig
        );

        ServletHolder jerseyHolder = new ServletHolder(new ServletContainer(resourceConfig));
        jerseyHolder.setInitOrder(0);
        apiContext.addServlet(jerseyHolder, "/*");

        // ========================================================
        // CONTEXTO 2: Archivos estáticos del frontend en /*
        // ========================================================
        ServletContextHandler staticContext = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        staticContext.setContextPath("/");

        // Buscar el directorio webapp en el classpath
        // Funciona tanto en desarrollo (target/classes/webapp/) como en el FAT JAR (!/webapp/)
        URL webappResource = Main.class.getClassLoader().getResource("webapp");
        String resourceBase;
        if (webappResource != null) {
            resourceBase = webappResource.toExternalForm();
        } else {
            // Fallback para desarrollo: directorio relativo al CWD
            resourceBase = "src/main/resources/webapp";
            LOGGER.warning("No se encontró 'webapp' en el classpath. Usando path de desarrollo: " + resourceBase);
        }

        staticContext.setResourceBase(resourceBase);

        ServletHolder defaultServlet = new ServletHolder("default", DefaultServlet.class);
        defaultServlet.setInitParameter("dirAllowed", "false");
        defaultServlet.setInitParameter("welcomeServlets", "false");
        defaultServlet.setInitParameter("gzip", "true");
        defaultServlet.setInitParameter("etags", "true");
        staticContext.addServlet(defaultServlet, "/");

        // ========================================================
        // COMBINAR CONTEXTOS
        // ========================================================
        ContextHandlerCollection contexts = new ContextHandlerCollection(apiContext, staticContext);
        server.setHandler(contexts);

        // ========================================================
        // INICIAR SERVIDOR
        // ========================================================
        // Verificar conexión a BD antes de arrancar (fail-fast)
        try {
            DatabaseConnection.getInstance().getConnection();
            LOGGER.info("Conexión a la base de datos: OK");
        } catch (Exception e) {
            LOGGER.severe("No se pudo conectar a la base de datos: " + e.getMessage());
            LOGGER.severe("Asegúrate de configurar DB_URL, DB_USER y DB_PASSWORD en el archivo .env o variables de entorno.");
            System.exit(1);
        }

        server.start();

        LOGGER.info("=================================================");
        LOGGER.info("  LoopFlow corriendo en http://localhost:" + port);
        LOGGER.info("  Frontend : http://localhost:" + port + "/");
        LOGGER.info("  API REST : http://localhost:" + port + "/api/");
        LOGGER.info("=================================================");

        // Hook de apagado limpio
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Apagando LoopFlow...");
            try {
                server.stop();
                DatabaseConnection.getInstance().closeConnection();
                LOGGER.info("Servidor detenido.");
            } catch (Exception e) {
                LOGGER.warning("Error al apagar el servidor: " + e.getMessage());
            }
        }));

        server.join(); // Bloquear el hilo principal mientras el servidor corre
    }
}
