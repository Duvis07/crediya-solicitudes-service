package co.com.crediya.solicitudes.aws.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualNotificationDto {

    @JsonProperty("applicationId")
    private Long applicationId;

    @JsonProperty("documentId")
    private String documentId;

    @JsonProperty("customerName")
    private String customerName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("loanType")
    private String loanType;

    @JsonProperty("amount")
    private Double amount;

    @JsonProperty("termMonths")
    private Integer termMonths;

    @JsonProperty("decision")
    private String decision; // "APPROVED" or "REJECTED"

    @JsonProperty("previousStatus")
    private String previousStatus;

    @JsonProperty("newStatus")
    private String newStatus;

    @JsonProperty("comments")
    private String comments;

    @JsonProperty("reason")
    private String reason; // Motivo del rechazo o aprobación

    @JsonProperty("interestRate")
    private Double interestRate;

    @JsonProperty("processedAt")
    private String processedAt;

    @JsonProperty("notificationType")
    private String notificationType; // "MANUAL_DECISION"
}
