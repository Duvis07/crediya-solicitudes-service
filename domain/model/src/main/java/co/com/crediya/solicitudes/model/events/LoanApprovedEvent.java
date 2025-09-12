package co.com.crediya.solicitudes.model.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApprovedEvent {
    private String solicitudId;
    private String clientEmail;
    private BigDecimal approvedAmount;
    private LocalDateTime approvedDate;
    private String eventType;

    public static final String EVENT_TYPE_LOAN_APPROVED = "LOAN_APPROVED";

    public static LoanApprovedEvent createLoanApprovedEvent(String solicitudId, String clientEmail, BigDecimal approvedAmount) {
        return LoanApprovedEvent.builder()
                .solicitudId(solicitudId)
                .clientEmail(clientEmail)
                .approvedAmount(approvedAmount)
                .approvedDate(LocalDateTime.now())
                .eventType(EVENT_TYPE_LOAN_APPROVED)
                .build();
    }
}
