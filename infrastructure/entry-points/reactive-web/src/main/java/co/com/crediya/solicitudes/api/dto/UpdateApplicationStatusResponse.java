package co.com.crediya.solicitudes.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateApplicationStatusResponse {
    
    @JsonProperty("application_id")
    private Long applicationId;
    
    @JsonProperty("previous_status")
    private String previousStatus;
    
    @JsonProperty("new_status")
    private String newStatus;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @JsonProperty("comments")
    private String comments;
}
