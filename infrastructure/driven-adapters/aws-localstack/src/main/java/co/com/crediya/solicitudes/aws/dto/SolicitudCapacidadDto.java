package co.com.crediya.solicitudes.aws.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudCapacidadDto {

    @JsonProperty("solicitud_id")
    private String solicitudId;

    @JsonProperty("documento_identidad")
    private String documentoIdentidad;

    @JsonProperty("monto")
    private BigDecimal monto;

    @JsonProperty("plazo_meses")
    private Integer plazoMeses;

    @JsonProperty("tasa_interes_anual")
    private BigDecimal tasaInteresAnual;

    @JsonProperty("salario_base")
    private BigDecimal salarioBase;

    @JsonProperty("tipo_prestamo")
    private String tipoPrestamo;

    @JsonProperty("email")
    private String email;

    @JsonProperty("nombre_completo")
    private String nombreCompleto;

    @JsonProperty("timestamp")
    private Long timestamp;
}
