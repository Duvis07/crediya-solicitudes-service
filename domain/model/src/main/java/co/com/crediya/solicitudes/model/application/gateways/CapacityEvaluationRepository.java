package co.com.crediya.solicitudes.model.application.gateways;

import co.com.crediya.solicitudes.model.application.Application;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Gateway para evaluación automática de capacidad de endeudamiento
 */
public interface CapacityEvaluationRepository {

    /**
     * Envía una solicitud para evaluación automática de capacidad
     * @param application La solicitud a evaluar
     * @return Mono con el ID del mensaje en la cola
     */
    Mono<String> enviarParaEvaluacionAutomatica(Application application);

    /**
     * Procesa el resultado de la evaluación de capacidad
     * @param solicitudId ID de la solicitud
     * @param decision Decisión tomada (APROBADO, RECHAZADO, REVISION_MANUAL)
     * @param motivo Motivo de la decisión
     * @param capacidadDisponible Capacidad disponible calculada
     * @param cuotaCalculada Cuota mensual calculada
     * @return Mono con la solicitud actualizada
     */
    Mono<Application> procesarResultadoEvaluacion(
        String solicitudId,
        String decision,
        String motivo,
        BigDecimal capacidadDisponible,
        BigDecimal cuotaCalculada
    );

    /**
     * Verifica si la validación automática está habilitada para un tipo de préstamo
     * @param loanTypeId ID del tipo de préstamo
     * @return true si está habilitada la validación automática
     */
    Mono<Boolean> esValidacionAutomaticaHabilitada(Long loanTypeId);
}
