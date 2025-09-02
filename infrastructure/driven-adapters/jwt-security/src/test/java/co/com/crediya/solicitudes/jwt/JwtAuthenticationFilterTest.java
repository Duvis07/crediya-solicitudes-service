package co.com.crediya.solicitudes.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private WebFilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private SecretKey secretKey;
    private static final String JWT_SECRET = "mySecretKeyForTestingPurposesOnly123456789";

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(JWT_SECRET);
        secretKey = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void filterShouldAllowPublicEndpointsWhenAccessingSwagger() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/swagger-ui/index.html"));

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void filterShouldAllowPublicEndpointsWhenAccessingApiDocs() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/v3/api-docs"));

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void filterShouldAllowPublicEndpointsWhenAccessingHealthCheck() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health"));

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void filterShouldReturnUnauthorizedWhenNoAuthorizationHeader() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/solicitud"));

        // Act
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filterShouldReturnUnauthorizedWhenInvalidAuthorizationHeader() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/solicitud")
                        .header(HttpHeaders.AUTHORIZATION, "Invalid token"));

        // Act
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filterShouldReturnUnauthorizedWhenInvalidJwtToken() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/solicitud")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token"));

        // Act
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filterShouldAllowAccessWhenValidJwtToken() {
        // Arrange
        String validToken = createValidJwtToken("user123", "ADMIN");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/solicitud")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken));

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        // Verify user attributes are set
        assertEquals("user123", exchange.getAttribute("userId"));
        assertEquals("ADMIN", exchange.getAttribute("userRole"));
    }

    @Test
    void filterShouldReturnUnauthorizedWhenExpiredJwtToken() {
        // Arrange
        String expiredToken = createExpiredJwtToken("user123", "ADMIN");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/solicitud")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken));

        // Act
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filterShouldSetCorrectUserAttributesWhenValidTokenWithAsesorRole() {
        // Arrange
        String validToken = createValidJwtToken("asesor456", "ASESOR");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken));

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals("asesor456", exchange.getAttribute("userId"));
        assertEquals("ASESOR", exchange.getAttribute("userRole"));
    }

    private String createValidJwtToken(String userId, String role) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact();
    }

    private String createExpiredJwtToken(String userId, String role) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .setIssuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
                .setExpiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact();
    }
}