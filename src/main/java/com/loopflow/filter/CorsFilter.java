package com.loopflow.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Filtro JAX-RS que añade cabeceras CORS a todas las respuestas de la API.
 *
 * <p>Configuración:
 * <ul>
 *   <li>{@code ALLOWED_ORIGINS} (env var): orígenes permitidos, separados por coma.
 *       Por defecto: {@code *} (todos, solo para desarrollo).</li>
 *   <li>En producción, establece {@code ALLOWED_ORIGINS=https://tu-app.netlify.app}.</li>
 * </ul>
 *
 * <p>Las solicitudes OPTIONS de preflight son respondidas inmediatamente con 200 OK
 * para que el navegador obtenga las cabeceras CORS antes de la solicitud real.
 */
@Provider
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String ALLOWED_ORIGINS_ENV = "ALLOWED_ORIGINS";
    private static final String DEFAULT_ORIGINS = "*";

    private static final String ALLOWED_METHODS = "GET, POST, PUT, PATCH, DELETE, OPTIONS";
    private static final String ALLOWED_HEADERS = "Content-Type, Authorization, Accept, X-Requested-With";
    private static final String MAX_AGE = "3600";

    /**
     * Intercepta peticiones OPTIONS (preflight) y las responde inmediatamente
     * con 200 OK para que el navegador reciba las cabeceras CORS.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            requestContext.abortWith(Response.ok().build());
        }
    }

    /**
     * Añade las cabeceras CORS a la respuesta.
     * Si ALLOWED_ORIGINS contiene múltiples orígenes (separados por coma),
     * se valida el origen de la solicitud y se devuelve solo el permitido.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {

        String allowedOriginsConfig = getEnvOrDefault(ALLOWED_ORIGINS_ENV, DEFAULT_ORIGINS);

        // Si es wildcard, simplifica las cabeceras
        if ("*".equals(allowedOriginsConfig.trim())) {
            responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
        } else {
            // Validar el origen de la solicitud contra la lista de permitidos
            String requestOrigin = requestContext.getHeaderString("Origin");
            Set<String> allowedSet = new HashSet<>(
                    Arrays.asList(allowedOriginsConfig.split(","))
            );
            // Limpiar espacios en los orígenes configurados
            Set<String> cleanedSet = new HashSet<>();
            for (String o : allowedSet) cleanedSet.add(o.trim());

            if (requestOrigin != null && cleanedSet.contains(requestOrigin)) {
                responseContext.getHeaders().add("Access-Control-Allow-Origin", requestOrigin);
                responseContext.getHeaders().add("Vary", "Origin");
            } else {
                // Origen no permitido — no añadir cabecera (el navegador bloqueará)
                return;
            }
        }

        responseContext.getHeaders().add("Access-Control-Allow-Methods", ALLOWED_METHODS);
        responseContext.getHeaders().add("Access-Control-Allow-Headers", ALLOWED_HEADERS);
        responseContext.getHeaders().add("Access-Control-Max-Age", MAX_AGE);
        responseContext.getHeaders().add("Access-Control-Allow-Credentials", "false");
    }

    private String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
