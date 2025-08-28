package co.com.crediya.solicitudes.api.dto;

import co.com.crediya.solicitudes.model.loantype.LoanTypeEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApplicationRequest {
    
    @NotBlank(message = "Document ID is required")
    @Size(min = 8, max = 15, message = "Document ID must be between 8 and 15 characters")
    @Pattern(regexp = "^\\d+$", message = "Document ID must contain only numbers")
    @JsonProperty("documentId")
    private String documentId;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @JsonProperty("email")
    private String email;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100000.0", message = "Amount must be at least 100,000")
    @DecimalMax(value = "50000000.0", message = "Amount cannot exceed 50,000,000")
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @NotNull(message = "Term is required")
    @Min(value = 6, message = "Term must be at least 6 months")
    @Max(value = 60, message = "Term cannot exceed 60 months")
    @JsonProperty("term")
    private Integer term;
    
    @NotNull(message = "Loan type is required")
    @JsonProperty("loanType")
    private LoanTypeEnum loanType;
}
