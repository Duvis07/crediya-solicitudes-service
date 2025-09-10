package co.com.crediya.solicitudes.aws.adapter;

import co.com.crediya.solicitudes.aws.sqs.SqsService;
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
public class ManualNotificationAdapter implements ManualNotificationRepository {

    private final SqsService sqsService;
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
                                String fullName = buildFullName(userResponse.getFirstName(), userResponse.getLastName());
                                log.info("Sending manual notification with: applicationId={}, fullName={}, newStatus={}", 
                                        application.getApplicationId(), fullName, newStatus);
                                return sqsService.sendManualNotificationWithUserData(
                                        application.getApplicationId(),
                                        application.getDocumentId(),
                                        application.getEmail(),
                                        fullName,
                                        newStatus,
                                        comments,
                                        reason
                                );
                            })
                            .onErrorResume(error -> {
                                log.warn("Could not retrieve user info for documentId {}, using default name: {}", 
                                        application.getDocumentId(), error.getMessage());
                                log.info("Fallback: Sending manual notification with: applicationId={}, fullName=Cliente, newStatus={}", 
                                        application.getApplicationId(), newStatus);
                                return Mono.fromCallable(() -> sqsService.sendManualNotificationWithUserData(
                                        application.getApplicationId(),
                                        application.getDocumentId(),
                                        application.getEmail(),
                                        "Cliente",
                                        newStatus,
                                        comments,
                                        reason
                                ));
                            })
                            .flatMap(mono -> mono)
                )
                .then();
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
}
