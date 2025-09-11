package co.com.crediya.solicitudes.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ApplicationDetailResponse {
    
    // Application ID
    @JsonProperty("application_id")
    private final Long applicationId;
    
    // Main application fields
    @JsonProperty("amount")
    private final BigDecimal amount;
    
    @JsonProperty("term")
    private final Integer term;
    
    @JsonProperty("application_status")
    private final String applicationState;
    
    // Client information
    @JsonProperty("document_id")
    private final String documentId;
    
    @JsonProperty("email")
    private final String email;
    
    @JsonProperty("full_name")
    private final String fullName;
    
    @JsonProperty("base_salary")
    private final BigDecimal baseSalary;
    
    // Loan information
    @JsonProperty("loan_type_name")
    private final String loanTypeName;
    
    @JsonProperty("interest_rate")
    private final BigDecimal interestRate;
    
    // Financial calculation
    @JsonProperty("total_monthly_debt_approved_applications")
    private final BigDecimal totalMonthlyDebt;
}
