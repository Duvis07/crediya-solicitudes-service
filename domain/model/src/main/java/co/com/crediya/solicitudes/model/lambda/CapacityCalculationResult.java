package co.com.crediya.solicitudes.model.lambda;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class CapacityCalculationResult {
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
