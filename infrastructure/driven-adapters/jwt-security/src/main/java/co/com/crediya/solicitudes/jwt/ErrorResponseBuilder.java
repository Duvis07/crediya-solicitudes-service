package co.com.crediya.solicitudes.jwt;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class ErrorResponseBuilder {

    private static final String CONTENT_TYPE_JSON = "application/json";

    public Mono<Void> buildUnauthorizedResponse(ServerWebExchange exchange, String message) {
        return buildErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized", message);
    }

    public Mono<Void> buildForbiddenResponse(ServerWebExchange exchange, String message) {
        return buildErrorResponse(exchange, HttpStatus.FORBIDDEN, "Forbidden", message);
    }

    private Mono<Void> buildErrorResponse(ServerWebExchange exchange, HttpStatus status, String error, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", CONTENT_TYPE_JSON);
        
        String errorBody = createErrorBody(error, status.value(), message);
        
        org.springframework.core.io.buffer.DataBuffer buffer = exchange.getResponse()
            .bufferFactory().wrap(errorBody.getBytes(StandardCharsets.UTF_8));
        
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String createErrorBody(String error, int status, String message) {
        return String.format(
            "{\"error\":\"%s\",\"status\":%d,\"message\":\"%s\"}", 
            error, status, escapeJsonString(message)
        );
    }

    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\"", "\\\"")
                   .replace("\\", "\\\\")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
