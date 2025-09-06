package co.com.crediya.solicitudes.aws.lambda;

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
public class LambdaCapacidadResponse {

    @JsonProperty("statusCode")
    private Integer statusCode;

    @JsonProperty("body")
    private String body;

    @JsonProperty("headers")
    private Map<String, String> headers;

    // Campos del body parseado
    @JsonProperty("solicitud_id")
    private String solicitudId;

    @JsonProperty("decision")
    private String decision;

    @JsonProperty("capacidad_disponible")
    private BigDecimal capacidadDisponible;

    @JsonProperty("cuota_calculada")
    private BigDecimal cuotaCalculada;

    @JsonProperty("motivo")
    private String motivo;

    @JsonProperty("plan_pagos")
    private List<Map<String, Object>> planPagos;

    @JsonProperty("error")
    private String error;

    @JsonProperty("message")
    private String message;

    /**
     * Verifica si la respuesta es exitosa
     */
    public boolean isExitosa() {
        return statusCode != null && statusCode >= 200 && statusCode < 300;
    }

    /**
     * Obtiene la decisión de la evaluación
     */
    public String getDecisionEvaluacion() {
        return decision != null ? decision : "RECHAZADO";
    }

    /**
     * Verifica si requiere revisión manual
     */
    public boolean requiereRevisionManual() {
        return "REVISION_MANUAL".equals(decision);
    }

    /**
     * Verifica si fue aprobado automáticamente
     */
    public boolean esAprobadoAutomatico() {
        return "APROBADO".equals(decision);
    }

    /**
     * Verifica si fue rechazado
     */
    public boolean esRechazado() {
        return "RECHAZADO".equals(decision);
    }
}
