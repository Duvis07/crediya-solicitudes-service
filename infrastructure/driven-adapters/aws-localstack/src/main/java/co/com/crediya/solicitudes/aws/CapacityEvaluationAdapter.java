package co.com.crediya.solicitudes.aws;

import co.com.crediya.solicitudes.aws.sqs.SqsService;
import co.com.crediya.solicitudes.aws.sqs.SolicitudCapacidadDto;
import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.application.gateways.ApplicationRepository;
import co.com.crediya.solicitudes.model.application.gateways.CapacityEvaluationRepository;
import co.com.crediya.solicitudes.model.loantype.gateways.LoanTypeRepository;
import co.com.crediya.solicitudes.model.state.gateways.StateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CapacityEvaluationAdapter implements CapacityEvaluationRepository {

    private final SqsService sqsService;
    private final ApplicationRepository applicationRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final StateRepository stateRepository;

    @Override
    public Mono<String> enviarParaEvaluacionAutomatica(Application application) {
        log.info("Preparando solicitud {} para evaluación automática", application.getApplicationId());
        
        return Mono.fromCallable(() -> {
            SolicitudCapacidadDto solicitudDto = SolicitudCapacidadDto.builder()
                .solicitudId(String.valueOf(application.getApplicationId()))
                .documentoIdentidad(application.getDocumentId())
                .monto(application.getAmount())
                .plazoMeses(application.getTerm())
                .tasaInteresAnual(BigDecimal.valueOf(12.0)) // Tasa por defecto
                .salarioBase(BigDecimal.valueOf(2000000)) // Salario por defecto
                .tipoPrestamo(obtenerTipoPrestamo(application.getLoanTypeId()))
                .email(application.getEmail())
                .nombreCompleto("Cliente") // Nombre por defecto
                .timestamp(System.currentTimeMillis())
                .build();
            
            return solicitudDto;
        })
        .flatMap(sqsService::enviarSolicitudParaEvaluacion)
        .doOnSuccess(messageId -> log.info("Solicitud {} enviada a SQS con messageId: {}", 
            application.getApplicationId(), messageId))
        .doOnError(error -> log.error("Error enviando solicitud {} a SQS: {}", 
            application.getApplicationId(), error.getMessage()));
    }

    @Override
    public Mono<Application> procesarResultadoEvaluacion(
            String solicitudId, 
            String decision, 
            String motivo, 
            BigDecimal capacidadDisponible, 
            BigDecimal cuotaCalculada) {
        
        log.info("Procesando resultado de evaluación para solicitud {}: {}", solicitudId, decision);
        
        return applicationRepository.findById(Long.valueOf(solicitudId))
            .flatMap(application -> {
                // Actualizar el estado según la decisión
                String nuevoEstado = mapearDecisionAEstado(decision);
                
                return stateRepository.findByName(nuevoEstado)
                    .flatMap(state -> {
                        Application applicationActualizada = application.toBuilder()
                            .stateId(state.getStateId())
                            .updatedAt(LocalDateTime.now())
                            .build();
                        
                        return applicationRepository.save(applicationActualizada);
                    })
                    .doOnSuccess(app -> log.info("Solicitud {} actualizada con decisión: {} (Estado: {})", 
                            solicitudId, decision, nuevoEstado))
                    .doOnError(error -> log.error("Error actualizando solicitud {}: {}", 
                            solicitudId, error.getMessage()));
            })
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Solicitud no encontrada: " + solicitudId)));
    }

    @Override
    public Mono<Boolean> esValidacionAutomaticaHabilitada(Long loanTypeId) {
        // Por ahora, habilitamos validación automática para todos los tipos de préstamo
        // En el futuro esto podría venir de configuración en base de datos
        log.info("Verificando validación automática para tipo de préstamo: {}", loanTypeId);
        
        return loanTypeRepository.findById(loanTypeId)
            .map(loanType -> {
                // Lógica de negocio: validación automática habilitada para ciertos tipos
                boolean habilitada = true; // Por defecto habilitada
                log.info("Validación automática {} para tipo de préstamo: {}", 
                    habilitada ? "habilitada" : "deshabilitada", loanType.getName());
                return habilitada;
            })
            .defaultIfEmpty(false)
            .doOnError(error -> log.error("Error verificando validación automática: {}", error.getMessage()));
    }

    private String obtenerTipoPrestamo(Long loanTypeId) {
        // Mapeo simple por ahora, en el futuro podría ser más sofisticado
        return "PERSONAL"; // Valor por defecto
    }

    private String mapearDecisionAEstado(String decision) {
        return switch (decision) {
            case "APROBADO" -> "Aprobada";
            case "RECHAZADO" -> "Rechazada";
            case "REVISION_MANUAL" -> "Revision manual";
            default -> "Pendiente de revision";
        };
    }
}
