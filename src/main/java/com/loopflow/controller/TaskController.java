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
    public Response getAllTasks(
            @QueryParam("status") String statusParam,
            @QueryParam("priority") String priorityParam) {
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
    public Response getTaskById(@PathParam("id") int id) {
        try {
            return Response.ok(service.getTaskById(id)).build();
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        }
    }

    // --- POST /api/tasks ---
    @POST
    public Response createTask(Task task) {
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
    public Response updateTask(@PathParam("id") int id, Task task) {
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
    public Response moveTask(@PathParam("id") int id, Map<String, String> body) {
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
    public Response getTaskHistory(@PathParam("id") int id) {
        try {
            return Response.ok(service.getTaskHistory(id)).build();
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        }
    }

    // --- DELETE /api/tasks/{id} ---
    @DELETE
    @Path("/{id}")
    public Response deleteTask(@PathParam("id") int id) {
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
