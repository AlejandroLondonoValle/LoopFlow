package com.loopflow.controller;

import com.loopflow.config.DatabaseConnection;
import com.loopflow.dao.impl.DailyLogDAOImpl;
import com.loopflow.dao.impl.HabitDAOImpl;
import com.loopflow.dao.impl.TaskDAOImpl;
import com.loopflow.service.DashboardService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Controlador REST del Dashboard principal.
 *
 * <p>Base path: {@code /api/dashboard}
 */
@Path("/dashboard")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardController {

    private final DashboardService service;

    public DashboardController() {
        var conn = DatabaseConnection.getInstance().getConnection();
        this.service = new DashboardService(
                new HabitDAOImpl(conn),
                new DailyLogDAOImpl(conn),
                new TaskDAOImpl(conn)
        );
    }

    /**
     * GET /api/dashboard
     *
     * <p>Retorna el resumen unificado del día:
     * <ul>
     *   <li>Fecha actual</li>
     *   <li>Hábitos activos con estado completado/pendiente del día</li>
     *   <li>Contadores de hábitos completados y pendientes</li>
     *   <li>Tareas agrupadas por columna Kanban (TODO, IN_PROGRESS, DONE)</li>
     * </ul>
     */
    @GET
    public Response getDashboard() {
        DashboardService.DashboardSummary summary = service.getDashboardSummary();
        return Response.ok(summary).build();
    }
}
