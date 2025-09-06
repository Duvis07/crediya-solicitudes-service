package co.com.crediya.solicitudes.aws.sqs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoEvaluacionDto {

    @JsonProperty("solicitudId")
    private String solicitudId;

    @JsonProperty("documentoIdentidad")
    private String documentoIdentidad;

    @JsonProperty("decision")
    private String decision; // APROBADO, RECHAZADO, REVISION_MANUAL

    @JsonProperty("motivo")
    private String motivo;

    @JsonProperty("capacidadDisponible")
    private BigDecimal capacidadDisponible;

    @JsonProperty("cuotaCalculada")
    private BigDecimal cuotaCalculada;

    @JsonProperty("planPagos")
    private List<Map<String, Object>> planPagos;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("email")
    private String email;

    @JsonProperty("nombreCompleto")
    private String nombreCompleto;

    @JsonProperty("montoAprobado")
    private BigDecimal montoAprobado;

    @JsonProperty("tasaInteresAnual")
    private BigDecimal tasaInteresAnual;

    @JsonProperty("plazoMeses")
    private Integer plazoMeses;

    @JsonProperty("cuotaMensual")
    private BigDecimal cuotaMensual;

    @JsonProperty("observaciones")
    private String observaciones;
}
