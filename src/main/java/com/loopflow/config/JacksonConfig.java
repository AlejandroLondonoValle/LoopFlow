package com.loopflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

/**
 * Proveedor JAX-RS que configura Jackson para serializar/deserializar correctamente:
 * <ul>
 *   <li>{@link java.time.LocalDate} como {@code "2024-06-15"}</li>
 *   <li>{@link java.time.LocalDateTime} como {@code "2024-06-15T08:30:00"}</li>
 *   <li>Enums como strings (comportamiento por defecto de Jackson).</li>
 * </ul>
 *
 * <p>Jersey descubre esta clase automáticamente via el escaneo de paquetes
 * configurado en {@code Main.java} ({@code resourceConfig.packages(...)}).
 */
@Provider
public class JacksonConfig implements ContextResolver<ObjectMapper> {

    private final ObjectMapper objectMapper;

    public JacksonConfig() {
        objectMapper = new ObjectMapper();

        // Registrar el módulo JSR-310 para soporte de Java 8 Time API
        objectMapper.registerModule(new JavaTimeModule());

        // Serializar fechas como strings ISO-8601, NO como timestamps numéricos
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // No fallar en propiedades desconocidas al deserializar (tolerancia al cambio)
        objectMapper.configure(
            com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false
        );
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }
}
