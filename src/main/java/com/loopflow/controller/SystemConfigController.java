package com.loopflow.controller;

import com.loopflow.config.DatabaseConnection;
import com.loopflow.dao.impl.SystemConfigDAOImpl;
import com.loopflow.model.SystemConfig;
import com.loopflow.service.SystemConfigService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la configuración del sistema.
 *
 * <p>Base path: {@code /api/config}
 */
@Path("/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Configuración", description = "Endpoints para la gestión de variables de entorno globales, parámetros internos y llaves operacionales de LoopFlow")
public class SystemConfigController {

    private final SystemConfigService service;

    public SystemConfigController() {
        this.service = new SystemConfigService(
                new SystemConfigDAOImpl(DatabaseConnection.getInstance().getConnection())
        );
    }

    // --- GET /api/config ---
    @GET
    @Operation(
        summary = "Listar todas las configuraciones globales",
        description = "Recupera una colección completa de todos los pares clave-valor de configuración técnica y del sistema guardados en la base de datos."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Parámetros de configuración del sistema obtenidos con éxito",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SystemConfig.class)))
        )
    })
    public Response getAllConfigs() {
        List<SystemConfig> configs = service.getAllConfigs();
        return Response.ok(configs).build();
    }

    // --- GET /api/config/{key} ---
    @GET
    @Path("/{key}")
    @Operation(
        summary = "Obtener una configuración específica por clave",
        description = "Busca y devuelve el valor y descripción de una variable del sistema a partir de su identificador único de texto (key)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Configuración encontrada de manera exitosa",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SystemConfig.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "La clave de configuración solicitada no existe en el sistema",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        )
    })
    public Response getConfig(
        @PathParam("key") 
        @Parameter(description = "Clave de texto identificadora de la configuración", required = true, example = "APP_THEME") 
        String key
    ) {
        try {
            SystemConfig config = service.getConfigByKey(key);
            return Response.ok(config).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new CategoryController.ErrorResponse(e.getMessage())).build();
        }
    }

    // --- PUT /api/config/{key} ---
    @PUT
    @Path("/{key}")
    @Operation(
        summary = "Establecer o actualizar el valor de una configuración",
        description = "Inserta una nueva clave o actualiza una existente con su respectivo valor. Es de carácter obligatorio pasar el atributo 'value'. Adicionalmente, se puede proveer un campo opcional 'description'."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Configuración guardada o modificada correctamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SystemConfig.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Error en la validación, el cuerpo está vacío o falta el campo 'value' obligatorio",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        )
    })
    public Response setConfig(
        @PathParam("key") 
        @Parameter(description = "Clave única asociada a la configuración a procesar", required = true, example = "MAX_HABITS_LIMIT") 
        String key, 
        @Parameter(
            description = "Estructura JSON con los datos del valor y su respectiva descripción opcional", 
            required = true,
            examples = @ExampleObject(value = "{\n  \"value\": \"10\",\n  \"description\": \"Límite máximo de hábitos activos por usuario\"\n}")
        ) 
        Map<String, String> body
    ) {
        if (body == null || !body.containsKey("value")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new CategoryController.ErrorResponse("Se requiere el campo 'value'")).build();
        }
        try {
            String value = body.get("value");
            String description = body.getOrDefault("description", null);
            SystemConfig config = service.setConfig(key, value, description);
            return Response.ok(config).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new CategoryController.ErrorResponse(e.getMessage())).build();
        }
    }

    // --- DELETE /api/config/{key} ---
    @DELETE
    @Path("/{key}")
    @Operation(
        summary = "Eliminar permanentemente una configuración",
        description = "Borra físicamente del repositorio la propiedad global del sistema vinculada a la clave especificada."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204", 
            description = "Parámetro de configuración borrado de forma correcta sin contenido de respuesta"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "No se localizó ninguna configuración con la clave ingresada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        )
    })
    public Response deleteConfig(
        @PathParam("key") 
        @Parameter(description = "Clave de la configuración que se desea remover", required = true, example = "MAX_HABITS_LIMIT") 
        String key
    ) {
        try {
            service.deleteConfig(key);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new CategoryController.ErrorResponse(e.getMessage())).build();
        }
    }
}