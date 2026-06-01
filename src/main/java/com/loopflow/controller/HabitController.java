package com.loopflow.controller;

import com.loopflow.config.DatabaseConnection;
import com.loopflow.dao.impl.DailyLogDAOImpl;
import com.loopflow.dao.impl.HabitDAOImpl;
import com.loopflow.model.DailyLog;
import com.loopflow.model.Habit;
import com.loopflow.service.HabitService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la entidad {@link Habit}.
 *
 * <p>Base path: {@code /api/habits}
 */
@Path("/habits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Hábitos", description = "Endpoints para la creación, seguimiento, archivado y métricas estadísticas de hábitos")
public class HabitController {

    private final HabitService service;

    public HabitController() {
        var conn = DatabaseConnection.getInstance().getConnection();
        this.service = new HabitService(
                new HabitDAOImpl(conn),
                new DailyLogDAOImpl(conn)
        );
    }

    // --- GET /api/habits ---
    @GET
    @Operation(
        summary = "Listar hábitos",
        description = "Obtiene la lista de hábitos. Puede filtrarse opcionalmente para retornar únicamente aquellos que estén activos."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Lista de hábitos recuperada con éxito",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Habit.class)))
        )
    })
    public Response getAllHabits(
        @QueryParam("active") 
        @Parameter(description = "Si es true, retorna solo hábitos activos. Si es false o se omite, retorna todos.", required = false) 
        Boolean activeOnly
    ) {
        List<Habit> habits = Boolean.TRUE.equals(activeOnly)
                ? service.getActiveHabits()
                : service.getAllHabits();
        return Response.ok(habits).build();
    }

    // --- GET /api/habits/{id} ---
    @GET
    @Path("/{id}")
    @Operation(
        summary = "Obtener un hábito por ID",
        description = "Busca y retorna la información detallada de un hábito específico según su ID numérico."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Hábito encontrado de forma exitosa",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Habit.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "El hábito con el ID proporcionado no existe",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        )
    })
    public Response getHabitById(
        @PathParam("id") 
        @Parameter(description = "ID numérico del hábito a consultar", required = true, example = "1") 
        int id
    ) {
        try {
            Habit habit = service.getHabitById(id);
            return Response.ok(habit).build();
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        }
    }

    // --- GET /api/habits/{id}/stats ---
    @GET
    @Path("/{id}/stats")
    @Operation(
        summary = "Obtener estadísticas de un hábito",
        description = "Calcula la racha actual (streak) y el porcentaje de cumplimiento (completion rate) de un hábito en un rango de fechas. Si no se especifican los parámetros, se evalúan por defecto los últimos 30 días."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Estadísticas calculadas correctamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(description = "Mapa de resultados estadísticos"),
                examples = @ExampleObject(value = "{\n  \"habitId\": 1,\n  \"streak\": 5,\n  \"completionRate\": 83.3,\n  \"from\": \"2026-05-01\",\n  \"to\": \"2026-05-31\"\n}")
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Formato de fecha inválido o error en los parámetros de entrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        )
    })
    public Response getHabitStats(
            @PathParam("id") @Parameter(description = "ID numérico del hábito", required = true, example = "1") int id,
            @QueryParam("from") @Parameter(description = "Fecha inicial en formato ISO (YYYY-MM-DD)", required = false, example = "2026-05-01") String fromStr,
            @QueryParam("to") @Parameter(description = "Fecha final en formato ISO (YYYY-MM-DD)", required = false, example = "2026-05-31") String toStr) {
        try {
            LocalDate from = fromStr != null ? LocalDate.parse(fromStr) : LocalDate.now().minusDays(30);
            LocalDate to   = toStr   != null ? LocalDate.parse(toStr)   : LocalDate.now();
            int streak = service.calculateStreak(id);
            double rate = service.getCompletionRate(id, from, to);
            return Response.ok(Map.of(
                    "habitId", id,
                    "streak", streak,
                    "completionRate", rate,
                    "from", from.toString(),
                    "to", to.toString()
            )).build();
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // --- POST /api/habits ---
    @POST
    @Operation(
        summary = "Crear un nuevo hábito",
        description = "Registra un hábito en el sistema vinculándolo a una categoría si es requerido."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "Hábito guardado correctamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Habit.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Estructura inválida o cuerpo de solicitud vacío",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        )
    })
    public Response createHabit(
        @Parameter(description = "Objeto JSON con las propiedades del nuevo hábito", required = true) 
        Habit habit
    ) {
        if (habit == null) return badRequest("Cuerpo de solicitud vacío");
        try {
            Habit created = service.createHabit(habit);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // --- PUT /api/habits/{id} ---
    @PUT
    @Path("/{id}")
    @Operation(
        summary = "Actualizar un hábito existente",
        description = "Modifica los datos de un hábito identificado por su ID."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Hábito actualizado correctamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Habit.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Cuerpo vacío o inconsistencia en los atributos del objeto",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "El hábito que se intenta modificar no existe",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        )
    })
    public Response updateHabit(
        @PathParam("id") @Parameter(description = "ID del hábito a modificar", required = true, example = "1") int id, 
        @Parameter(description = "Objeto JSON con los campos actualizados del hábito", required = true) Habit habit
    ) {
        if (habit == null) return badRequest("Cuerpo de solicitud vacío");
        try {
            Habit updated = service.updateHabit(id, habit);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            return e.getMessage().contains("no encontrado")
                    ? notFound(e.getMessage())
                    : badRequest(e.getMessage());
        }
    }

    // --- DELETE /api/habits/{id} ---
    @DELETE
    @Path("/{id}")
    @Operation(
        summary = "Eliminar permanentemente un hábito",
        description = "Remueve un hábito del sistema por completo utilizando su ID."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204", 
            description = "Hábito removido exitosamente sin retornar contenido"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Hábito no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        )
    })
    public Response deleteHabit(
        @PathParam("id") 
        @Parameter(description = "ID del hábito a eliminar", required = true, example = "1") 
        int id
    ) {
        try {
            service.deleteHabit(id);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        }
    }

    // --- PATCH /api/habits/{id}/archive ---
    @PATCH
    @Path("/{id}/archive")
    @Operation(
        summary = "Archivar un hábito",
        description = "Desactiva un hábito (cambia su estado interno activo a false) para removerlo del día a día sin perder su historial."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Hábito archivado de forma exitosa",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Habit.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Hábito no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        )
    })
    public Response archiveHabit(
        @PathParam("id") 
        @Parameter(description = "ID del hábito a archivar", required = true, example = "1") 
        int id
    ) {
        try {
            Habit habit = service.setHabitActive(id, false);
            return Response.ok(habit).build();
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        }
    }

    // --- POST /api/habits/{id}/complete ---
    @POST
    @Path("/{id}/complete")
    @Operation(
        summary = "Registrar cumplimiento del día (Check/Uncheck)",
        description = "Marca un hábito como completado o incompleto para el día de hoy, permitiendo además añadir anotaciones u observaciones opcionales."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Registro diario guardado o actualizado con éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DailyLog.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "El hábito referenciado no existe en el sistema",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        )
    })
    public Response markCompleted(
        @PathParam("id") @Parameter(description = "ID del hábito", required = true, example = "1") int id, 
        @Parameter(
            description = "Estructura JSON que define el estado y notas", 
            required = true,
            examples = @ExampleObject(value = "{\n  \"completed\": true,\n  \"notes\": \"Completado por la mañana temprano\"\n}")
        ) Map<String, Object> body
    ) {
        try {
            boolean completed = Boolean.TRUE.equals(body != null ? body.get("completed") : true);
            String notes = body != null ? (String) body.get("notes") : null;
            DailyLog log = service.markTodayCompleted(id, completed, notes);
            return Response.ok(log).build();
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        }
    }

    // --- GET /api/habits/{id}/logs ---
    @GET
    @Path("/{id}/logs")
    @Operation(
        summary = "Consultar el historial de logs de un hábito",
        description = "Recupera todos los registros diarios de marcas de cumplimiento asociados a un hábito en particular."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Historial de logs recuperado con éxito",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = DailyLog.class)))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Hábito no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        )
    })
    public Response getHabitLogs(
        @PathParam("id") 
        @Parameter(description = "ID del hábito del cual se quieren revisar los registros históricos", required = true, example = "1") 
        int id
    ) {
        try {
            List<DailyLog> logs = service.getLogsForHabit(id);
            return Response.ok(logs).build();
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        }
    }

    // --- Helpers ---
    private Response notFound(String msg) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new CategoryController.ErrorResponse(msg)).build();
    }

    private Response badRequest(String msg) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new CategoryController.ErrorResponse(msg)).build();
    }
}