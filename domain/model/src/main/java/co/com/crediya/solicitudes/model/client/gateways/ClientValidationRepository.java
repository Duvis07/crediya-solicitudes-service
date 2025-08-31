package co.com.crediya.solicitudes.model.client.gateways;

import reactor.core.publisher.Mono;

public interface ClientValidationRepository {
    
    Mono<String> getUserEmailByDocumentId(String documentId);
}
