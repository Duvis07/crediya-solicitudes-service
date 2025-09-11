package co.com.crediya.solicitudes.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleAuthorizationFilterTest {

    @Mock
    private WebFilterChain filterChain;

    @Mock
    private ErrorResponseBuilder errorResponseBuilder;

    private RoleAuthorizationFilter roleAuthorizationFilter;

    @BeforeEach
    void setUp() {
        roleAuthorizationFilter = new RoleAuthorizationFilter(errorResponseBuilder);
    }

    @Test
    void filterShouldAllowAccessWhenPathIsNotSolicitudEndpoint() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/usuarios"));

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = roleAuthorizationFilter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void filterShouldAllowAccessWhenMethodIsNotGet() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/solicitud"));

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = roleAuthorizationFilter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void filterShouldAllowAccessWhenUserHasAsesorRole() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/solicitud"));
        exchange.getAttributes().put("userRole", "ASESOR");

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = roleAuthorizationFilter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void filterShouldReturnForbiddenWhenUserRoleIsNull() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/solicitud"));
        // No userRole attribute set

        when(errorResponseBuilder.buildForbiddenResponse(any(), any())).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = roleAuthorizationFilter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "APPLICANT", "INVALIDROLE"})
    void filterShouldReturnForbiddenWhenUserHasUnauthorizedRole(String role) {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/solicitud"));
        exchange.getAttributes().put("userRole", role);

        when(errorResponseBuilder.buildForbiddenResponse(any(), any())).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = roleAuthorizationFilter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void filterShouldAllowAccessWhenGetRequestToSolicitudWithAsesorRole() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, "/api/v1/solicitud")
                        .queryParam("page", "0")
                        .queryParam("size", "10"));
        exchange.getAttributes().put("userRole", "ASESOR");

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = roleAuthorizationFilter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void filterShouldAllowAccessWhenDifferentEndpointWithAnyRole() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/usuarios/12345678"));
        exchange.getAttributes().put("userRole", "ADMIN");

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = roleAuthorizationFilter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void filterShouldAllowAccessWhenPostRequestToSolicitudWithAnyRole() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/solicitud"));
        exchange.getAttributes().put("userRole", "APPLICANT");

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = roleAuthorizationFilter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }
}
