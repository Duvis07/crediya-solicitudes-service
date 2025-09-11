package co.com.crediya.solicitudes.usecase.capacity;

import co.com.crediya.solicitudes.model.application.gateways.CapacityEvaluationRepository;
import co.com.crediya.solicitudes.model.client.gateways.ClientValidationRepository;
import co.com.crediya.solicitudes.model.exceptions.ClientNotFoundException;
import co.com.crediya.solicitudes.model.lambda.CapacityCalculationRequest;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.logging.Logger;


@RequiredArgsConstructor
public class CapacityCalculationUseCase {

    private final CapacityEvaluationRepository capacityEvaluationRepository;
    private final ClientValidationRepository clientValidationRepository;

    private static final Logger log = Logger.getLogger(CapacityCalculationUseCase.class.getName());

    public Mono<String> sendForCapacityCalculation(CapacityCalculationRequest request) {
        log.info("Starting capacity calculation for document: " + request.getDocumentoIdentidad());
        
        return validateClient(request)
                .then(capacityEvaluationRepository.sendForCapacityCalculation(request))
                .doOnSuccess(messageId -> log.info("Capacity calculation request sent to SQS with messageId: {}"))
                .doOnError(error -> log.severe("Error sending capacity calculation request to SQS: " + error.getMessage()));
    }
    
    private Mono<Void> validateClient(CapacityCalculationRequest request) {
        log.info("Validating client exists: " + request.getDocumentoIdentidad());

        return clientValidationRepository.getUserEmailByDocumentId(request.getDocumentoIdentidad())
                .switchIfEmpty(Mono.error(new ClientNotFoundException("Client not found with documentId: " + request.getDocumentoIdentidad())))
                .flatMap(userEmail -> {
                    String requestEmail = request.getEmail() != null ? request.getEmail().trim() : null;
                    log.info("Email from auth service: " + userEmail + " - Email from request: " + requestEmail);
                    // Validate ownership - user can only calculate capacity for themselves
                    if (!userEmail.equals(requestEmail)) {
                        log.severe("OWNERSHIP DENIED: User with documentId " + request.getDocumentoIdentidad() +
                                " attempted to use incorrect email. Expected: " + userEmail + ", Provided: " + requestEmail);
                        return Mono.error(new ClientNotFoundException(
                                "Access denied: You can only calculate capacity for yourself. " +
                                        "The email provided does not match the registered user."));
                    }

                    log.info("Client validation successful for documentId: " + request.getDocumentoIdentidad());
                    return Mono.empty();
                });
    }
}
