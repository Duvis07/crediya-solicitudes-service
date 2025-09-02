package co.com.crediya.solicitudes.api;

import co.com.crediya.solicitudes.api.dto.ApplicationDetailResponse;
import co.com.crediya.solicitudes.api.dto.CreateApplicationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@RequiredArgsConstructor
public class RouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/v1/solicitud",
                    method = RequestMethod.POST,
                    operation = @Operation(
                            operationId = "createApplication",
                            summary = "Crear solicitud de préstamo",
                            description = "Registra una nueva solicitud de préstamo con información del cliente y detalles del préstamo",
                            tags = {"Solicitudes"},
                            requestBody = @RequestBody(
                                    description = "Datos de la solicitud de préstamo",
                                    required = true,
                                    content = @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = CreateApplicationRequest.class)
                                    )
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Solicitud creada exitosamente",
                                            content = @Content(
                                                    mediaType = "application/json",
                                                    schema = @Schema(implementation = String.class)
                                            )
                                    ),
                                    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
                                    @ApiResponse(responseCode = "404", description = "Tipo de préstamo no encontrado"),
                                    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/solicitud",
                    method = RequestMethod.GET,
                    operation = @Operation(
                            operationId = "getAllApplications",
                            summary = "Obtener solicitudes para revisión manual",
                            description = "Recupera solicitudes paginadas que requieren revisión manual (Pendiente de revisión, Rechazadas, Revisión manual)",
                            tags = {"Solicitudes"},
                            parameters = {
                                    @io.swagger.v3.oas.annotations.Parameter(
                                            name = "page",
                                            description = "Número de página (comenzando desde 0)",
                                            required = false,
                                            schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")
                                    ),
                                    @io.swagger.v3.oas.annotations.Parameter(
                                            name = "size",
                                            description = "Tamaño de página (máximo 100)",
                                            required = false,
                                            schema = @Schema(type = "integer", defaultValue = "10", minimum = "1", maximum = "100")
                                    )
                            },
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Solicitudes recuperadas exitosamente",
                                            content = @Content(
                                                    mediaType = "application/json",
                                                    schema = @Schema(implementation = ApplicationDetailResponse.class)
                                            )
                                    ),
                                    @ApiResponse(responseCode = "400", description = "Parámetros de paginación inválidos"),
                                    @ApiResponse(responseCode = "401", description = "No autorizado - Se requiere autenticación"),
                                    @ApiResponse(responseCode = "403", description = "Prohibido - Solo asesores pueden acceder"),
                                    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route(POST("/api/v1/solicitud")
                        .and(accept(MediaType.APPLICATION_JSON))
                        .and(contentType(MediaType.APPLICATION_JSON)),
                handler::createApplication)
                .andRoute(GET("/api/v1/solicitud")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        handler::getAllApplications);
    }
}
