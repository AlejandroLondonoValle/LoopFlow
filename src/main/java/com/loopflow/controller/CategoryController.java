package com.loopflow.controller;

import com.loopflow.config.DatabaseConnection;
import com.loopflow.dao.impl.CategoryDAOImpl;
import com.loopflow.model.Category;
import com.loopflow.service.CategoryService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * Controlador REST para la entidad {@link Category}.
 *
 * <p>Base path: {@code /api/categories}
 */
@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoryController {

    private final CategoryService service;

    public CategoryController() {
        this.service = new CategoryService(
                new CategoryDAOImpl(DatabaseConnection.getInstance().getConnection())
        );
    }

    // --- GET /api/categories ---
    @GET
    public Response getAllCategories() {
        List<Category> categories = service.getAllCategories();
        return Response.ok(categories).build();
    }

    // --- GET /api/categories/{id} ---
    @GET
    @Path("/{id}")
    public Response getCategoryById(@PathParam("id") int id) {
        try {
            Category category = service.getCategoryById(id);
            return Response.ok(category).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    // --- POST /api/categories ---
    @POST
    public Response createCategory(Category category) {
        if (category == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("El cuerpo de la solicitud no puede estar vacío")).build();
        }
        try {
            Category created = service.createCategory(category);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    // --- PUT /api/categories/{id} ---
    @PUT
    @Path("/{id}")
    public Response updateCategory(@PathParam("id") int id, Category category) {
        if (category == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("El cuerpo de la solicitud no puede estar vacío")).build();
        }
        try {
            Category updated = service.updateCategory(id, category);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            String status = e.getMessage().contains("no encontrada") ? "404" : "400";
            return Response.status(Integer.parseInt(status))
                    .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    // --- DELETE /api/categories/{id} ---
    @DELETE
    @Path("/{id}")
    public Response deleteCategory(@PathParam("id") int id) {
        try {
            service.deleteCategory(id);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    // --- DTO de error ---
    public static class ErrorResponse {
        private final String error;
        public ErrorResponse(String error) { this.error = error; }
        public String getError() { return error; }
    }
}
