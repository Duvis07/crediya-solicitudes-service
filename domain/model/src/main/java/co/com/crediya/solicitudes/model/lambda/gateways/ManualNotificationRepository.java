package co.com.crediya.solicitudes.model.lambda.gateways;

import reactor.core.publisher.Mono;

public interface ManualNotificationRepository {
    
    Mono<Void> sendManualDecisionNotification(Long applicationId, String documentId, String email, 
                                            String previousStatus, String newStatus, String comments, String reason);
}
