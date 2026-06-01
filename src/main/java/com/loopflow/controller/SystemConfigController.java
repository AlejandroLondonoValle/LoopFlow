package com.loopflow.controller;

import com.loopflow.config.DatabaseConnection;
import com.loopflow.dao.impl.SystemConfigDAOImpl;
import com.loopflow.model.SystemConfig;
import com.loopflow.service.SystemConfigService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
public class SystemConfigController {

    private final SystemConfigService service;

    public SystemConfigController() {
        this.service = new SystemConfigService(
                new SystemConfigDAOImpl(DatabaseConnection.getInstance().getConnection())
        );
    }

    // --- GET /api/config ---
    @GET
    public Response getAllConfigs() {
        List<SystemConfig> configs = service.getAllConfigs();
        return Response.ok(configs).build();
    }

    // --- GET /api/config/{key} ---
    @GET
    @Path("/{key}")
    public Response getConfig(@PathParam("key") String key) {
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
    public Response setConfig(@PathParam("key") String key, Map<String, String> body) {
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
    public Response deleteConfig(@PathParam("key") String key) {
        try {
            service.deleteConfig(key);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new CategoryController.ErrorResponse(e.getMessage())).build();
        }
    }
}
