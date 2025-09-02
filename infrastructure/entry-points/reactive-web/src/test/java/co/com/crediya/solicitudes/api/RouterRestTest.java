package co.com.crediya.solicitudes.api;

import co.com.crediya.solicitudes.api.dto.ApplicationResponse;
import co.com.crediya.solicitudes.api.dto.ApplicationCreatedResponse;
import co.com.crediya.solicitudes.api.dto.CreateApplicationRequest;
import co.com.crediya.solicitudes.model.loantype.LoanTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouterRestTest {

    @Mock
    private Handler handler;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        RouterRest routerRest = new RouterRest();
        RouterFunction<ServerResponse> routerFunction = routerRest.routerFunction(handler);
        webTestClient = WebTestClient.bindToRouterFunction(routerFunction).build();
    }

    static Stream<Arguments> validCreateApplicationRequests() {
        return Stream.of(
                Arguments.of(
                        CreateApplicationRequest.builder()
                                .documentId("12345678")
                                .email("test@example.com")
                                .amount(new BigDecimal("500000"))
                                .term(24)
                                .loanType(LoanTypeEnum.PERSONAL)
                                .build()
                ),
                Arguments.of(
                        CreateApplicationRequest.builder()
                                .documentId("87654321")
                                .email("mortgage@example.com")
                                .amount(new BigDecimal("25000000"))
                                .term(36)
                                .loanType(LoanTypeEnum.MORTGAGE)
                                .build()
                ),
                Arguments.of(
                        CreateApplicationRequest.builder()
                                .documentId("11223344")
                                .email("vehicle@example.com")
                                .amount(new BigDecimal("8000000"))
                                .term(48)
                                .loanType(LoanTypeEnum.VEHICLE)
                                .build()
                )
        );
    }

    @ParameterizedTest
    @MethodSource("validCreateApplicationRequests")
    void createApplication_ShouldReturn200_WhenValidRequest(CreateApplicationRequest request) {
        // Arrange
        ApplicationCreatedResponse expectedResponse = ApplicationCreatedResponse.builder()
                .message("Solicitud de préstamo creada exitosamente")
                .status("CREATED")
                .timestamp(LocalDateTime.now())
                .build();

        when(handler.createApplication(any()))
                .thenReturn(ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(expectedResponse));

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/solicitud")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Solicitud de préstamo creada exitosamente")
                .jsonPath("$.status").isEqualTo("CREATED")
                .jsonPath("$.timestamp").exists();
    }

    @Test
    void createApplication_ShouldReturn415_WhenInvalidAcceptHeader() {
        // Arrange
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(24)
                .loanType(LoanTypeEnum.PERSONAL)
                .build();

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/solicitud")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_PLAIN) // Invalid accept header
                .bodyValue(request)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void createApplication_ShouldReturn500_WhenHandlerThrowsException() {
        // Arrange
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(24)
                .loanType(LoanTypeEnum.PERSONAL)
                .build();

        when(handler.createApplication(any()))
                .thenReturn(Mono.error(new RuntimeException("Internal server error")));

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/solicitud")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void getAllApplications_ShouldReturn200_WhenApplicationsExist() {
        // Arrange
        List<ApplicationResponse> applications = List.of(
                ApplicationResponse.builder()
                        .applicationId(1L)
                        .documentId("12345678")
                        .email("test1@example.com")
                        .amount(new BigDecimal("500000"))
                        .term(24)
                        .build(),
                ApplicationResponse.builder()
                        .applicationId(2L)
                        .documentId("87654321")
                        .email("test2@example.com")
                        .amount(new BigDecimal("1000000"))
                        .term(36)
                        .build()
        );

        when(handler.getAllApplications(any()))
                .thenReturn(ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(applications));

        // Act & Assert
        webTestClient.get()
                .uri("/api/v1/solicitud")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(ApplicationResponse.class)
                .hasSize(2);
    }

    @Test
    void getAllApplications_ShouldReturn200_WhenNoApplicationsExist() {
        // Arrange
        when(handler.getAllApplications(any()))
                .thenReturn(ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(List.of()));

        // Act & Assert
        webTestClient.get()
                .uri("/api/v1/solicitud")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(ApplicationResponse.class)
                .hasSize(0);
    }

    @Test
    void getAllApplications_ShouldReturn406_WhenInvalidAcceptHeader() {
        // Act & Assert
        webTestClient.get()
                .uri("/api/v1/solicitud")
                .accept(MediaType.TEXT_PLAIN) // Invalid accept header
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void getAllApplications_ShouldReturn500_WhenHandlerThrowsException() {
        // Arrange
        when(handler.getAllApplications(any()))
                .thenReturn(ServerResponse.status(500)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("Internal Server Error"));

        // Act & Assert
        webTestClient.get()
                .uri("/api/v1/solicitud")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void routerFunction_ShouldRejectInvalidPaths() {
        // Test invalid path
        webTestClient.get()
                .uri("/api/v1/invalid-path")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();

        // Test invalid version
        webTestClient.get()
                .uri("/api/v2/solicitud")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }
}
