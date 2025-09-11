package co.com.crediya.solicitudes.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtErrorDetector jwtErrorDetector;
    private final JwtErrorMessageProvider jwtErrorMessageProvider;
    private final ErrorResponseBuilder errorResponseBuilder;

    public boolean isJwtRelatedError(Throwable error) {
        return jwtErrorDetector.isJwtRelatedError(error);
    }

    public String getJwtErrorMessage(Throwable error) {
        return jwtErrorMessageProvider.getErrorMessage(error);
    }

    public Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        return errorResponseBuilder.buildUnauthorizedResponse(exchange, message);
    }
}
