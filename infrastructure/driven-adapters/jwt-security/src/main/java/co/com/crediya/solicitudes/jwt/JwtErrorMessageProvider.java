package co.com.crediya.solicitudes.jwt;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;

@Component
public class JwtErrorMessageProvider {

    private static final String DEFAULT_ERROR_MESSAGE = "Invalid JWT token";
    
    private final Map<Class<? extends Throwable>, Function<Throwable, String>> errorMessageMappings;

    public JwtErrorMessageProvider() {
        this.errorMessageMappings = initializeErrorMappings();
    }

    public String getErrorMessage(Throwable error) {
        return errorMessageMappings.entrySet().stream()
                .filter(entry -> entry.getKey().isInstance(error))
                .findFirst()
                .map(entry -> entry.getValue().apply(error))
                .orElse(DEFAULT_ERROR_MESSAGE);
    }

    private Map<Class<? extends Throwable>, Function<Throwable, String>> initializeErrorMappings() {
        return Map.of(
            io.jsonwebtoken.ExpiredJwtException.class, ex -> "JWT token has expired",
            io.jsonwebtoken.MalformedJwtException.class, ex -> "Invalid JWT token format",
            io.jsonwebtoken.UnsupportedJwtException.class, ex -> "Unsupported JWT token",
            io.jsonwebtoken.security.SecurityException.class, ex -> "Invalid JWT signature",
            IllegalArgumentException.class, ex -> "JWT token cannot be empty"
        );
    }
}
