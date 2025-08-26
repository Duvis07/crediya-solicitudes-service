package co.com.crediya.solicitudes.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private String error;
    private int status;
    private String message;
    private List<String> details;
    private LocalDateTime timestamp;
    
    public static ErrorResponse of(String error, int status, String message) {
        return ErrorResponse.builder()
                .error(error)
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ErrorResponse withDetails(String error, int status, String message, List<String> details) {
        return ErrorResponse.builder()
                .error(error)
                .status(status)
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
