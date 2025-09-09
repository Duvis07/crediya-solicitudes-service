package co.com.crediya.solicitudes.aws;

import co.com.crediya.solicitudes.aws.sqs.SqsService;
import co.com.crediya.solicitudes.aws.dto.SolicitudCapacidadDto;
import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.application.gateways.ApplicationRepository;
import co.com.crediya.solicitudes.model.application.gateways.CapacityEvaluationRepository;
import co.com.crediya.solicitudes.model.loantype.gateways.LoanTypeRepository;
import co.com.crediya.solicitudes.model.state.gateways.StateRepository;
import co.com.crediya.solicitudes.webclient.AuthServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CapacityEvaluationAdapter implements CapacityEvaluationRepository {

    private static final BigDecimal DEFAULT_INTEREST_RATE = BigDecimal.valueOf(12.0);
    private static final BigDecimal DEFAULT_BASE_SALARY = BigDecimal.valueOf(2000000);
    private static final String DEFAULT_LOAN_TYPE = "PERSONAL";

    private final SqsService sqsService;
    private final ApplicationRepository applicationRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final StateRepository stateRepository;
    private final AuthServiceClient authServiceClient;

    @Override
    public Mono<String> sendForAutomaticEvaluation(Application application) {
        log.info("Preparing application {} for automatic evaluation", application.getApplicationId());
        
        // Get user info from authentication service to obtain the real customer name
        return authServiceClient.getUserByDocumentId(application.getDocumentId())
                .map(userResponse -> {
                    // Build full name from firstName and lastName
                    String fullName = buildFullName(userResponse.getFirstName(), userResponse.getLastName());
                    
                    return SolicitudCapacidadDto.builder()
                            .solicitudId(String.valueOf(application.getApplicationId()))
                            .documentoIdentidad(application.getDocumentId())
                            .monto(application.getAmount())
                            .plazoMeses(application.getTerm())
                            .tasaInteresAnual(DEFAULT_INTEREST_RATE)
                            .salarioBase(DEFAULT_BASE_SALARY)
                            .tipoPrestamo(DEFAULT_LOAN_TYPE)
                            .email(application.getEmail())
                            .nombreCompleto(fullName)
                            .timestamp(System.currentTimeMillis())
                            .build();
                })
                .onErrorResume(error -> {
                    log.warn("Could not retrieve user info for documentId {}, using default name: {}", 
                            application.getDocumentId(), error.getMessage());
                    // Fallback to default name if auth service fails
                    return Mono.just(SolicitudCapacidadDto.builder()
                            .solicitudId(String.valueOf(application.getApplicationId()))
                            .documentoIdentidad(application.getDocumentId())
                            .monto(application.getAmount())
                            .plazoMeses(application.getTerm())
                            .tasaInteresAnual(DEFAULT_INTEREST_RATE)
                            .salarioBase(DEFAULT_BASE_SALARY)
                            .tipoPrestamo(DEFAULT_LOAN_TYPE)
                            .email(application.getEmail())
                            .nombreCompleto("Cliente")
                            .timestamp(System.currentTimeMillis())
                            .build());
                })
                .flatMap(sqsService::sendApplicationForEvaluation)
                .doOnSuccess(messageId -> log.info("Application {} sent to SQS with messageId: {}", 
                    application.getApplicationId(), messageId))
                .doOnError(error -> log.error("Error sending application {} to SQS: {}", 
                    application.getApplicationId(), error.getMessage()));
    }

    @Override
    public Mono<Application> processEvaluationResult(
            String applicationId, 
            String decision, 
            String reason, 
            BigDecimal availableCapacity, 
            BigDecimal calculatedInstallment) {
        
        log.info("Processing evaluation result for application {}: {}", applicationId, decision);
        
        return applicationRepository.findById(Long.valueOf(applicationId))
            .flatMap(application -> {
                // Update state based on decision
                String newState = mapDecisionToState(decision);
                
                return stateRepository.findByName(newState)
                    .flatMap(state -> {
                        Application updatedApplication = application.toBuilder()
                            .stateId(state.getStateId())
                            .updatedAt(LocalDateTime.now())
                            .build();
                        
                        return applicationRepository.save(updatedApplication);
                    })
                    .doOnSuccess(app -> log.info("Application {} updated with decision: {} (State: {})", 
                            applicationId, decision, newState))
                    .doOnError(error -> log.error("Error updating application {}: {}", 
                            applicationId, error.getMessage()));
            })
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Application not found: " + applicationId)));
    }

    @Override
    public Mono<Boolean> isAutomaticValidationEnabled(Long loanTypeId) {
        // For now, we enable automatic validation for all loan types
        // In the future this could come from database configuration
        log.info("Checking automatic validation for loan type: {}", loanTypeId);
        
        return loanTypeRepository.findById(loanTypeId)
            .map(loanType -> {
                // Business logic: automatic validation enabled for certain types
                boolean enabled = true; // Enabled by default
                log.info("Automatic validation {} for loan type: {}", 
                    enabled ? "enabled" : "disabled", loanType.getName());
                return enabled;
            })
            .defaultIfEmpty(false)
            .doOnError(error -> log.error("Error checking automatic validation: {}", error.getMessage()));
    }


    /**
     * Builds full name from first and last name with null safety
     */
    private String buildFullName(String firstName, String lastName) {
        if (firstName == null && lastName == null) {
            return "Cliente";
        }
        if (firstName == null) {
            return lastName.trim();
        }
        if (lastName == null) {
            return firstName.trim();
        }
        return (firstName.trim() + " " + lastName.trim()).trim();
    }

    private String mapDecisionToState(String decision) {
        return switch (decision) {
            case "APPROVED", "APROBADO" -> "Aprobada";
            case "REJECTED", "RECHAZADO" -> "Rechazada";
            case "MANUAL_REVIEW", "REVISION_MANUAL" -> "Revision manual";
            default -> "Pendiente de revision";
        };
    }
}
