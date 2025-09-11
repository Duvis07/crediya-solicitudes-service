package co.com.crediya.solicitudes.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateApplicationStatusRequest {
    
    @JsonProperty("status")
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(Aprobada|Rechazada)$", message = "Status must be 'Aprobada' or 'Rechazada'")
    private String status;
    
    @JsonProperty("comments")
    private String comments;
}
