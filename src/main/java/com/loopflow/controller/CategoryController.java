package com.loopflow.controller;

import com.loopflow.config.DatabaseConnection;
import com.loopflow.dao.impl.CategoryDAOImpl;
import com.loopflow.model.Category;
import com.loopflow.service.CategoryService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * Controlador REST para la entidad {@link Category}.
 *
 * <p>Base path: {@code /api/categories}
 */
@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Categorías", description = "Endpoints para la administración y organización de categorías de LoopFlow")
public class CategoryController {

    private final CategoryService service;

    public CategoryController() {
        this.service = new CategoryService(
                new CategoryDAOImpl(DatabaseConnection.getInstance().getConnection())
        );
    }

    // --- GET /api/categories ---
    @GET
    @Operation(
        summary = "Listar todas las categorías",
        description = "Recupera una lista completa con todas las categorías de hábitos registradas en el sistema."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Lista de categorías recuperada con éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))
        )
    })
    public Response getAllCategories() {
        List<Category> categories = service.getAllCategories();
        return Response.ok(categories).build();
    }

    // --- GET /api/categories/{id} ---
    @GET
    @Path("/{id}")
    @Operation(
        summary = "Obtener categoría por ID",
        description = "Busca y devuelve los detalles de una categoría específica utilizando su identificador único numérico."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Categoría encontrada exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "La categoría solicitada no existe en la base de datos",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public Response getCategoryById(
        @PathParam("id") 
        @Parameter(description = "ID numérico de la categoría a consultar", required = true, example = "1") 
        int id
    ) {
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
    @Operation(
        summary = "Crear una nueva categoría",
        description = "Registra una categoría en el sistema. Nota: No es necesario enviar el campo 'id' en el cuerpo, ya que la base de datos lo genera de forma autoincremental."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "Categoría creada con éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Error en la validación de los datos proporcionados o cuerpo de petición vacío",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public Response createCategory(
        @Parameter(description = "Objeto JSON con los datos de la nueva categoría", required = true) 
        Category category
    ) {
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
    @Operation(
        summary = "Actualizar una categoría existente",
        description = "Modifica los campos de una categoría ya almacenada en base a su ID único."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Categoría actualizada de forma correcta",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Datos de entrada incorrectos o cuerpo vacío",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "La categoría que se intenta modificar no fue encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public Response updateCategory(
        @PathParam("id") 
        @Parameter(description = "ID de la categoría a actualizar", required = true, example = "2") 
        int id, 
        @Parameter(description = "Objeto JSON con las propiedades modificadas de la categoría", required = true) 
        Category category
    ) {
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
    @Operation(
        summary = "Eliminar una categoría",
        description = "Remueve permanentemente una categoría de la base de datos a partir de su ID."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204", 
            description = "Categoría eliminada con éxito (No devuelve contenido de cuerpo)"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "La categoría indicada no existe",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public Response deleteCategory(
        @PathParam("id") 
        @Parameter(description = "ID numérico de la categoría a eliminar", required = true, example = "3") 
        int id
    ) {
        try {
            service.deleteCategory(id);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    // --- DTO de error ---
    @Schema(description = "Estructura estándar para las respuestas de error en la API")
    public static class ErrorResponse {
        @Schema(description = "Mensaje descriptivo del error ocurrido", example = "La categoría especificada no fue encontrada")
        private final String error;
        public ErrorResponse(String error) { this.error = error; }
        public String getError() { return error; }
    }
}