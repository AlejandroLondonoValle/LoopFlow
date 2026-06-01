package com.loopflow.controller;

import com.loopflow.config.DatabaseConnection;
import com.loopflow.dao.impl.TaskDAOImpl;
import com.loopflow.dao.impl.TaskHistoryDAOImpl;
import com.loopflow.model.Task;
import com.loopflow.model.enums.Priority;
import com.loopflow.model.enums.TaskStatus;
import com.loopflow.service.TaskService;
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
 * Controlador REST para la entidad {@link Task}.
 *
 * <p>Base path: {@code /api/tasks}
 */
@Path("/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Tareas", description = "Endpoints para la gestión de tareas del tablero Kanban, prioridades e historial de estados")
public class TaskController {

    private final TaskService service;

    public TaskController() {
        var conn = DatabaseConnection.getInstance().getConnection();
        this.service = new TaskService(
                new TaskDAOImpl(conn),
                new TaskHistoryDAOImpl(conn)
        );
    }

    // --- GET /api/tasks ---
    @GET
    @Operation(
        summary = "Listar tareas con filtros",
        description = "Retorna la lista de todas las tareas registradas. Permite aplicar un filtro opcional exclusivo por estado (status) o por prioridad (priority) mediante parámetros en la URL."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Colección de tareas obtenida con éxito",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Task.class)))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "El valor del filtro proporcionado no coincide con los ENUM permitidos",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        )
    })
    public Response getAllTasks(
            @QueryParam("status") 
            @Parameter(description = "Filtrar por estado del tablero (ej: TODO, IN_PROGRESS, DONE)", required = false, example = "TODO") 
            String statusParam,
            @QueryParam("priority") 
            @Parameter(description = "Filtrar por nivel de prioridad (ej: LOW, MEDIUM, HIGH)", required = false, example = "HIGH") 
            String priorityParam) {
        try {
            List<Task> tasks;
            if (statusParam != null && !statusParam.isBlank()) {
                tasks = service.getTasksByStatus(TaskStatus.valueOf(statusParam.toUpperCase()));
            } else if (priorityParam != null && !priorityParam.isBlank()) {
                tasks = service.getTasksByPriority(Priority.valueOf(priorityParam.toUpperCase()));
            } else {
                tasks = service.getAllTasks();
            }
            return Response.ok(tasks).build();
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // --- GET /api/tasks/{id} ---
    @GET
    @Path("/{id}")
    @Operation(
        summary = "Obtener una tarea por ID",
        description = "Busca y devuelve la información detallada de una tarea en base a su identificador numérico único."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Tarea encontrada con éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Task.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "La tarea con el ID especificado no existe",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        )
    })
    public Response getTaskById(
        @PathParam("id") 
        @Parameter(description = "ID numérico de la tarea a consultar", required = true, example = "1") 
        int id
    ) {
        try {
            return Response.ok(service.getTaskById(id)).build();
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        }
    }

    // --- POST /api/tasks ---
    @POST
    @Operation(
        summary = "Crear una nueva tarea",
        description = "Registra una nueva tarea dentro del sistema. Se le asigna un estado inicial por defecto (normalmente TODO) y se vincula a su respectiva categoría si se especifica."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "Tarea creada de forma exitosa",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Task.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Datos inválidos en el objeto JSON o cuerpo de la petición vacío",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        )
    })
    public Response createTask(
        @Parameter(description = "Objeto JSON con los atributos de la tarea a crear", required = true) 
        Task task
    ) {
        if (task == null) return badRequest("Cuerpo de solicitud vacío");
        try {
            Task created = service.createTask(task);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // --- PUT /api/tasks/{id} ---
    @PUT
    @Path("/{id}")
    @Operation(
        summary = "Actualizar una tarea existente",
        description = "Modifica los campos generales de una tarea ya guardada (título, descripción, prioridad, etc.) localizándola por su ID."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Tarea actualizada correctamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Task.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Cuerpo vacío o inconsistencia en los tipos de datos",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "La tarea que se desea modificar no existe",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        )
    })
    public Response updateTask(
        @PathParam("id") @Parameter(description = "ID de la tarea a actualizar", required = true, example = "1") int id, 
        @Parameter(description = "Objeto JSON con los datos modificados de la tarea", required = true) Task task
    ) {
        if (task == null) return badRequest("Cuerpo de solicitud vacío");
        try {
            Task updated = service.updateTask(id, task);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            return e.getMessage().contains("no encontrada")
                    ? notFound(e.getMessage())
                    : badRequest(e.getMessage());
        }
    }

    // --- PATCH /api/tasks/{id}/move ---
    @PATCH
    @Path("/{id}/move")
    @Operation(
        summary = "Mover tarea en el tablero Kanban (Cambiar estado)",
        description = "Modifica únicamente el estado operacional de una tarea (por ejemplo, transicionarla de 'TODO' a 'IN_PROGRESS' o 'DONE')."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Tarea desplazada con éxito en el flujo Kanban",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Task.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Falta el atributo 'status' en el cuerpo o el estado indicado es inválido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "La tarea especificada no fue encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        )
    })
    public Response moveTask(
        @PathParam("id") @Parameter(description = "ID de la tarea a mover", required = true, example = "1") int id, 
        @Parameter(
            description = "Objeto JSON que contiene el nuevo estado destino de la tarea", 
            required = true,
            examples = @ExampleObject(value = "{\n  \"status\": \"IN_PROGRESS\"\n}")
        ) Map<String, String> body
    ) {
        if (body == null || !body.containsKey("status")) {
            return badRequest("Se requiere el campo 'status' en el cuerpo");
        }
        try {
            TaskStatus newStatus = TaskStatus.valueOf(body.get("status").toUpperCase());
            Task moved = service.moveTask(id, newStatus);
            return Response.ok(moved).build();
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg.contains("no encontrada")) return notFound(msg);
            return badRequest(msg);
        }
    }

    // --- GET /api/tasks/{id}/history ---
    @GET
    @Path("/{id}/history")
    @Operation(
        summary = "Consultar historial de cambios de la tarea",
        description = "Recupera la bitácora histórica completa de movimientos de estados y modificaciones cronológicas asociadas a una tarea específica."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Historial transaccional recuperado correctamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(description = "Lista de eventos de historial de la tarea")))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "No se localizó la tarea o su historial",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        )
    })
    public Response getTaskHistory(
        @PathParam("id") 
        @Parameter(description = "ID de la tarea de la cual se desea auditar el historial", required = true, example = "1") 
        int id
    ) {
        try {
            return Response.ok(service.getTaskHistory(id)).build();
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        }
    }

    // --- DELETE /api/tasks/{id} ---
    @DELETE
    @Path("/{id}")
    @Operation(
        summary = "Eliminar una tarea permanentemente",
        description = "Borra físicamente del registro del sistema la tarea asociada al ID entregado."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204", 
            description = "Tarea eliminada exitosamente (no devuelve cuerpo de respuesta)"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "La tarea que se pretende eliminar no existe en el sistema",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryController.ErrorResponse.class))
        )
    })
    public Response deleteTask(
        @PathParam("id") 
        @Parameter(description = "ID de la tarea a eliminar permanentemente", required = true, example = "1") 
        int id
    ) {
        try {
            service.deleteTask(id);
            return Response.noContent().build();
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