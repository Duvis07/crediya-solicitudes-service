package co.com.crediya.solicitudes.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculateCapacityResponse {
    
    private String decision;
    private String motivo;
    private BigDecimal capacidadDisponible;
    private BigDecimal cuotaCalculada;
    private BigDecimal montoAprobado;
    private BigDecimal tasaInteresAnual;
    private Integer plazoMeses;
    private BigDecimal cuotaMensual;
    private List<Object> planPagos;
}
