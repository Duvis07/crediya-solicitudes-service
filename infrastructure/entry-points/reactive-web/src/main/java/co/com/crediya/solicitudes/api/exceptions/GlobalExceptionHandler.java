package co.com.crediya.solicitudes.api.exceptions;

import co.com.crediya.solicitudes.api.dto.ErrorResponse;
import co.com.crediya.solicitudes.model.exceptions.ApplicationNotFoundException;
import co.com.crediya.solicitudes.model.exceptions.InvalidApplicationDataException;
import co.com.crediya.solicitudes.model.exceptions.LoanTypeNotFoundException;
import co.com.crediya.solicitudes.model.exceptions.ClientNotFoundException;
import co.com.crediya.solicitudes.model.exceptions.ServiceUnavailableException;
import co.com.crediya.solicitudes.model.exceptions.UserServiceException;
import co.com.crediya.solicitudes.model.exceptions.InvalidStateTransitionException;
import co.com.crediya.solicitudes.model.exceptions.InvalidTargetStatusException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.springframework.core.codec.DecodingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
@Order(-2)
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private static final String VALIDATION_ERROR_MESSAGE = "Validation errors in request data";
    private static final String INVALID_FORMAT_MESSAGE = "Invalid ID format provided";
    private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred";
    private static final String FIELD_ERROR_FORMAT = "Field '%s' with value '%s': %s";

    private final ObjectMapper objectMapper;

    private final Map<Class<? extends Throwable>, Function<Throwable, ErrorMappingResult>> exceptionMappings = initializeExceptionMappings();

    private Map<Class<? extends Throwable>, Function<Throwable, ErrorMappingResult>> initializeExceptionMappings() {
        Map<Class<? extends Throwable>, Function<Throwable, ErrorMappingResult>> mappings = new java.util.HashMap<>();
        mappings.put(ApplicationNotFoundException.class, ex -> new ErrorMappingResult(HttpStatus.NOT_FOUND, ex.getMessage(), null));
        mappings.put(LoanTypeNotFoundException.class, ex -> new ErrorMappingResult(HttpStatus.NOT_FOUND, ex.getMessage(), null));
        mappings.put(InvalidApplicationDataException.class, ex -> new ErrorMappingResult(HttpStatus.BAD_REQUEST, ex.getMessage(), null));
        mappings.put(ClientNotFoundException.class, ex -> new ErrorMappingResult(HttpStatus.NOT_FOUND, ex.getMessage(), null));
        mappings.put(ServiceUnavailableException.class, ex -> new ErrorMappingResult(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), null));
        mappings.put(UserServiceException.class, ex -> new ErrorMappingResult(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), null));
        mappings.put(InvalidStateTransitionException.class, ex -> new ErrorMappingResult(HttpStatus.BAD_REQUEST, ex.getMessage(), null));
        mappings.put(InvalidTargetStatusException.class, ex -> new ErrorMappingResult(HttpStatus.BAD_REQUEST, ex.getMessage(), null));
        mappings.put(ValidationException.class, this::handleValidationException);
        mappings.put(NumberFormatException.class, ex -> new ErrorMappingResult(HttpStatus.BAD_REQUEST, INVALID_FORMAT_MESSAGE, null));
        mappings.put(ServerWebInputException.class, this::handleWebInputException);
        mappings.put(DecodingException.class, this::handleDecodingException);
        return mappings;
    }

    @Override
    @NonNull
    public Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        log.error("Global exception handler caught: {}", ex.getMessage(), ex);

        ErrorMappingResult errorResult = mapException(ex);
        ErrorResponse errorResponse = createErrorResponse(errorResult);

        return writeErrorResponse(exchange, errorResult.status(), errorResponse);
    }

    private ErrorMappingResult mapException(Throwable ex) {
        return exceptionMappings.entrySet().stream()
                .filter(entry -> entry.getKey().isInstance(ex))
                .findFirst()
                .map(entry -> entry.getValue().apply(ex))
                .orElse(new ErrorMappingResult(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR_MESSAGE, null));
    }

    private ErrorMappingResult handleValidationException(Throwable ex) {
        ValidationException validationEx = (ValidationException) ex;
        List<String> details = extractValidationDetails(validationEx);
        return new ErrorMappingResult(HttpStatus.BAD_REQUEST, VALIDATION_ERROR_MESSAGE, details);
    }

    private ErrorMappingResult handleWebInputException(Throwable ex) {
        return handleCauseChain(ex.getCause(), "Invalid request data");
    }

    private ErrorMappingResult handleDecodingException(Throwable ex) {
        return handleCauseChain(ex.getCause(), "Invalid request format");
    }

    private ErrorMappingResult handleCauseChain(Throwable cause, String fallbackMessage) {
        while (cause != null) {
            if (cause instanceof InvalidFormatException formatEx) {
                String fieldName = extractFieldFromPath(formatEx.getPath());
                String invalidValue = formatEx.getValue() != null ? formatEx.getValue().toString() : "null";
                String message = createCleanErrorMessage(fieldName, invalidValue, formatEx);
                return new ErrorMappingResult(HttpStatus.BAD_REQUEST, message, null);
            }
            if (cause instanceof InvalidApplicationDataException) {
                return new ErrorMappingResult(HttpStatus.BAD_REQUEST, cause.getMessage(), null);
            }
            cause = cause.getCause();
        }
        return new ErrorMappingResult(HttpStatus.BAD_REQUEST, fallbackMessage, null);
    }

    private String createCleanErrorMessage(String fieldName, String invalidValue, InvalidFormatException formatEx) {
        String originalMessage = formatEx.getOriginalMessage();

        if (originalMessage.contains("not one of the values accepted for Enum class:")) {
            int startIndex = originalMessage.indexOf("[");
            int endIndex = originalMessage.indexOf("]");
            if (startIndex != -1 && endIndex != -1) {
                String validValues = originalMessage.substring(startIndex, endIndex + 1);
                return String.format("Invalid value '%s' for field '%s'. Valid values are: %s",
                        invalidValue, fieldName, validValues);
            }
        }

        return String.format("Invalid value '%s' for field '%s'", invalidValue, fieldName);
    }

    private String extractFieldFromPath(java.util.List<JsonMappingException.Reference> path) {
        if (path != null && !path.isEmpty()) {
            JsonMappingException.Reference ref = path.get(path.size() - 1);
            return ref.getFieldName() != null ? ref.getFieldName() : "unknown field";
        }
        return "unknown field";
    }

    private List<String> extractValidationDetails(ValidationException validationException) {
        if (validationException.getErrors() == null || !validationException.getErrors().hasFieldErrors()) {
            return List.of();
        }

        List<String> errorDetails = validationException.getErrors().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();

        log.warn("Validation error details: {}", errorDetails);
        return errorDetails;
    }

    private String formatFieldError(FieldError fieldError) {
        return String.format(FIELD_ERROR_FORMAT,
                fieldError.getField(),
                fieldError.getRejectedValue(),
                fieldError.getDefaultMessage());
    }

    private ErrorResponse createErrorResponse(ErrorMappingResult errorResult) {
        if (errorResult.details() != null && !errorResult.details().isEmpty()) {
            return ErrorResponse.withDetails(
                    errorResult.status().getReasonPhrase(),
                    errorResult.status().value(),
                    errorResult.message(),
                    errorResult.details()
            );
        } else {
            return ErrorResponse.of(
                    errorResult.status().getReasonPhrase(),
                    errorResult.status().value(),
                    errorResult.message()
            );
        }
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, ErrorResponse errorResponse) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        try {
            String body = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory()
                    .wrap(body.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error serializing error response", e);
            return exchange.getResponse().setComplete();
        }
    }

    private record ErrorMappingResult(HttpStatus status, String message, List<String> details) {
    }
}
