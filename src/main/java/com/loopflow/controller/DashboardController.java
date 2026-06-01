package com.loopflow.controller;

import com.loopflow.config.DatabaseConnection;
import com.loopflow.dao.impl.DailyLogDAOImpl;
import com.loopflow.dao.impl.HabitDAOImpl;
import com.loopflow.dao.impl.TaskDAOImpl;
import com.loopflow.service.DashboardService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST del Dashboard principal.
 *
 * <p>Base path: {@code /api/dashboard}
 */
@Path("/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Dashboard", description = "Endpoints para el panel de control principal y resúmenes unificados de productividad")
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
     * <p>Retorna el resumen unificado del día.
     */
    @GET
    @Operation(
        summary = "Obtener el resumen general del día",
        description = "Retorna una vista unificada optimizada para la pantalla principal que incluye: la fecha actual, la lista de hábitos activos con su respectivo estado de cumplimiento diario, métricas de progreso (contadores) y las tareas organizadas por columnas del tablero Kanban (TODO, IN_PROGRESS, DONE)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Resumen del Dashboard generado y recuperado con éxito",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = DashboardService.DashboardSummary.class)
            )
        )
    })
    public Response getDashboard() {
        DashboardService.DashboardSummary summary = service.getDashboardSummary();
        return Response.ok(summary).build();
    }
}