package co.com.crediya.solicitudes.model.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Application {
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
