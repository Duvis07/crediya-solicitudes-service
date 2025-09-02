package co.com.crediya.solicitudes.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {
    
    private Long applicationId;
    private String documentId;
    private BigDecimal amount;
    private Integer term;
    private String email;
    private Long stateId;
    private Long loanTypeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
