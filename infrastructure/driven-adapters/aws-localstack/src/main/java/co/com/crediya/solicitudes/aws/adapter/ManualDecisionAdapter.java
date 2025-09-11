package co.com.crediya.solicitudes.aws.adapter;

import co.com.crediya.solicitudes.aws.sqs.MessageQueueService;
import co.com.crediya.solicitudes.aws.utils.UserNameUtils;
import co.com.crediya.solicitudes.model.application.gateways.ApplicationRepository;
import co.com.crediya.solicitudes.usecase.gateways.ManualNotificationRepository;
import co.com.crediya.solicitudes.webclient.AuthServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManualDecisionAdapter implements ManualNotificationRepository {

    private final MessageQueueService messageQueueService;
    private final ApplicationRepository applicationRepository;
    private final AuthServiceClient authServiceClient;

    @Override
    public Mono<Void> sendManualDecisionNotification(Long applicationId, String documentId, String email, 
                                                    String previousStatus, String newStatus, String comments, String reason) {
        log.info("Processing manual decision notification for application: {}", applicationId);
        
        // Get application data and user info to send complete notification
        return applicationRepository.findById(applicationId)
                .doOnNext(application -> log.info("Found application: ID={}, DocumentId={}, Email={}", 
                        application.getApplicationId(), application.getDocumentId(), application.getEmail()))
                .flatMap(application -> 
                    authServiceClient.getUserByDocumentId(application.getDocumentId())
                            .doOnNext(userResponse -> log.info("Retrieved user info: firstName={}, lastName={}", 
                                    userResponse.getFirstName(), userResponse.getLastName()))
                            .map(userResponse -> {
                                String fullName = UserNameUtils.buildFullName(userResponse.getFirstName(), userResponse.getLastName());
                                String decision = mapStatusToDecision(newStatus);
                                log.info("Sending manual notification with: applicationId={}, fullName={}, newStatus={} -> decision={}", 
                                        application.getApplicationId(), fullName, newStatus, decision);
                                return messageQueueService.sendManualNotificationWithUserData(
                                        application.getApplicationId(),
                                        application.getDocumentId(),
                                        application.getEmail(),
                                        fullName,
                                        decision,
                                        comments,
                                        reason
                                );
                            })
                            .onErrorResume(error -> {
                                log.error("Failed to retrieve user info for documentId {}: {}", 
                                        application.getDocumentId(), error.getMessage());
                                return Mono.error(new RuntimeException("Unable to retrieve user information for manual notification", error));
                            })
                            .flatMap(mono -> mono)
                )
                .then();
    }

    /**
     * Maps application status to decision format expected by SQS consumer
     */
    private String mapStatusToDecision(String status) {
        return switch (status.toLowerCase()) {
            case "aprobada", "approved" -> "APROBADA";
            case "rechazada", "rejected" -> "RECHAZADA";
            case "pendiente de revision", "revision manual", "manual review" -> "REVISION_MANUAL";
            default -> status.toUpperCase();
        };
    }

}
