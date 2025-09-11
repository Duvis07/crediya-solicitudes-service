package co.com.crediya.solicitudes.model.capacity;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder(toBuilder = true)
public class CapacityEvaluationRequest {
    private Long applicationId;
    private String documentoIdentidad;
    private String email;
    private BigDecimal monto;
    private Integer plazoMeses;
    private BigDecimal tasaInteresAnual;
    private BigDecimal salarioBase;
    private String nombreCompleto;
}
