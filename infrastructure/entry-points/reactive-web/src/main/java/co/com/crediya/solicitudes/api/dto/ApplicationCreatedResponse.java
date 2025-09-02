package co.com.crediya.solicitudes.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCreatedResponse {
    private String message;
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    public static ApplicationCreatedResponse success() {
        return ApplicationCreatedResponse.builder()
                .message("Solicitud de préstamo creada exitosamente")
                .status("CREATED")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
