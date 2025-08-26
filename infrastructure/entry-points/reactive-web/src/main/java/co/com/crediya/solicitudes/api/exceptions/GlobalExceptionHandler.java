package co.com.crediya.solicitudes.api.exceptions;

import co.com.crediya.solicitudes.model.exceptions.ApplicationNotFoundException;
import co.com.crediya.solicitudes.model.exceptions.InvalidApplicationDataException;
import co.com.crediya.solicitudes.model.exceptions.LoanTypeNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Order(-2)
@Slf4j
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        log.error("Global exception handler caught: {}", ex.getMessage(), ex);

        HttpStatus status;
        String message;

        String details = null;

        if (ex instanceof ApplicationNotFoundException) {
            status = HttpStatus.NOT_FOUND;
            message = ex.getMessage();
        } else if (ex instanceof LoanTypeNotFoundException) {
            status = HttpStatus.BAD_REQUEST;
            message = ex.getMessage();
        } else if (ex instanceof InvalidApplicationDataException) {
            status = HttpStatus.BAD_REQUEST;
            message = ex.getMessage();
        } else if (ex instanceof ValidationException) {
            status = HttpStatus.BAD_REQUEST;
            message = "Validation errors in request data";
            details = extractValidationDetails((ValidationException) ex);
        } else if (ex instanceof NumberFormatException) {
            status = HttpStatus.BAD_REQUEST;
            message = "Invalid ID format provided";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "An unexpected error occurred";
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        String body = buildErrorResponse(status, message, details);

        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String extractValidationDetails(ValidationException validationException) {
        if (validationException.getErrors() == null || !validationException.getErrors().hasFieldErrors()) {
            return null;
        }

        List<String> errorDetails = validationException.getErrors().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());

        log.warn("Validation error details: {}", errorDetails);

        return "[" + errorDetails.stream()
                .map(detail -> "\"" + detail.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",")) + "]";
    }

    private String formatFieldError(FieldError fieldError) {
        return String.format("Field '%s' with value '%s': %s",
                fieldError.getField(),
                fieldError.getRejectedValue(),
                fieldError.getDefaultMessage());
    }

    private String buildErrorResponse(HttpStatus status, String message, String details) {
        if (details != null) {
            return String.format(
                    "{\"error\":\"%s\",\"status\":%d,\"message\":\"%s\",\"details\":%s,\"timestamp\":\"%s\"}",
                    status.getReasonPhrase(),
                    status.value(),
                    message,
                    details,
                    java.time.LocalDateTime.now()
            );
        } else {
            return String.format(
                    "{\"error\":\"%s\",\"status\":%d,\"message\":\"%s\",\"timestamp\":\"%s\"}",
                    status.getReasonPhrase(),
                    status.value(),
                    message,
                    java.time.LocalDateTime.now()
            );
        }
    }
}
