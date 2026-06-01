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
    public Response getAllHabits(@QueryParam("active") Boolean activeOnly) {
        List<Habit> habits = Boolean.TRUE.equals(activeOnly)
                ? service.getActiveHabits()
                : service.getAllHabits();
        return Response.ok(habits).build();
    }

    // --- GET /api/habits/{id} ---
    @GET
    @Path("/{id}")
    public Response getHabitById(@PathParam("id") int id) {
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
    public Response getHabitStats(
            @PathParam("id") int id,
            @QueryParam("from") String fromStr,
            @QueryParam("to") String toStr) {
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
    public Response createHabit(Habit habit) {
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
    public Response updateHabit(@PathParam("id") int id, Habit habit) {
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
    public Response deleteHabit(@PathParam("id") int id) {
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
    public Response archiveHabit(@PathParam("id") int id) {
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
    public Response markCompleted(@PathParam("id") int id, Map<String, Object> body) {
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
    public Response getHabitLogs(@PathParam("id") int id) {
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
