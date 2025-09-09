package co.com.crediya.solicitudes.model.application.gateways;

import co.com.crediya.solicitudes.model.application.Application;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;


public interface CapacityEvaluationRepository {

    Mono<String> sendForAutomaticEvaluation(Application application);


    Mono<Application> processEvaluationResult(
            String applicationId,
            String decision,
            String reason,
            BigDecimal availableCapacity,
            BigDecimal calculatedInstallment
    );


    Mono<Boolean> isAutomaticValidationEnabled(Long loanTypeId);
}
