package co.com.crediya.solicitudes.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculateCapacityRequest {
    
    private String documentoIdentidad;
    private BigDecimal monto;
    private Integer plazoMeses;
    private BigDecimal tasaInteresAnual;
    private BigDecimal salarioBase;
    private String tipoPrestamo;
    private String email;
}
