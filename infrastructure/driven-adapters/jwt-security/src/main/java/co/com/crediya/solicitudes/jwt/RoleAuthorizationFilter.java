package co.com.crediya.solicitudes.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(2) // Execute after JwtAuthenticationFilter
public class RoleAuthorizationFilter implements WebFilter {

    private final ErrorResponseBuilder errorResponseBuilder;

    private static final String USER_ROLE_ATTRIBUTE = "userRole";
    private static final String SOLICITUD_ENDPOINT = "/api/v1/solicitud";
    private static final String GET_METHOD = "GET";
    private static final Set<String> ALLOWED_ROLES = Set.of(
            AllowedRoles.ASESOR.name()
    );

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        // Only apply role validation to GET /api/v1/solicitud
        if (!shouldValidateRole(path, method)) {
            return chain.filter(exchange);
        }

        String userRole = exchange.getAttribute(USER_ROLE_ATTRIBUTE);

        if (userRole == null) {
            log.warn("No user role found in request attributes for path: {}", path);
            return errorResponseBuilder.buildForbiddenResponse(exchange, "Access denied. ASESOR role required");
        }

        if (!ALLOWED_ROLES.contains(userRole)) {
            log.warn("Access denied for user with role '{}' to path: {}", userRole, path);
            return errorResponseBuilder.buildForbiddenResponse(exchange, "Access denied. ASESOR role required");
        }

        log.debug("Role authorization successful for user with role: {} on path: {}", userRole, path);
        return chain.filter(exchange);
    }

    private boolean shouldValidateRole(String path, String method) {
        return (GET_METHOD.equals(method) || "PUT".equals(method)) && 
               (SOLICITUD_ENDPOINT.equals(path) || path.startsWith(SOLICITUD_ENDPOINT + "/"));
    }

}
