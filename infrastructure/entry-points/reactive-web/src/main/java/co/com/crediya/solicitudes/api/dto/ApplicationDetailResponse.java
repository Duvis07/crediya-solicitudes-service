package co.com.crediya.solicitudes.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ApplicationDetailResponse {
    
    // Campos principales de la solicitud
    @JsonProperty("monto")
    private final BigDecimal amount;
    
    @JsonProperty("plazo")
    private final Integer term;
    
    @JsonProperty("estado_solicitud")
    private final String applicationState;
    
    // Información del cliente
    @JsonProperty("documento_id")
    private final String documentId;
    
    @JsonProperty("email")
    private final String email;
    
    @JsonProperty("nombre")
    private final String fullName;
    
    @JsonProperty("salario_base")
    private final BigDecimal baseSalary;
    
    // Información del préstamo
    @JsonProperty("tipo_prestamo")
    private final String loanTypeName;
    
    @JsonProperty("tasa_interes")
    private final BigDecimal interestRate;
    
    // Cálculo financiero
    @JsonProperty("deuda_total_mensual_solicitudes_aprobadas")
    private final BigDecimal totalMonthlyDebt;
}
