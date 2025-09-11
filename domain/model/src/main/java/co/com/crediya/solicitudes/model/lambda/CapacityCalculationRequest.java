package co.com.crediya.solicitudes.model.lambda;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapacityCalculationRequest {
    private String documentoIdentidad;
    private String email;
    private BigDecimal monto;
    private Integer plazoMeses;
    private BigDecimal tasaInteresAnual;
    private BigDecimal salarioBase;
}
