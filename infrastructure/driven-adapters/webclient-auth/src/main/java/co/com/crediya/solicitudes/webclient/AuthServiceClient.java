package co.com.crediya.solicitudes.webclient;

import co.com.crediya.solicitudes.model.client.gateways.ClientValidationRepository;
import co.com.crediya.solicitudes.model.exceptions.ClientNotFoundException;
import co.com.crediya.solicitudes.model.exceptions.ServiceUnavailableException;
import co.com.crediya.solicitudes.webclient.config.AuthServiceEndpoints;
import co.com.crediya.solicitudes.webclient.dto.UserResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class AuthServiceClient implements ClientValidationRepository {

    private final WebClient webClient;
    private final String authServiceBaseUrl;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;

    @Override
    public Mono<Boolean> validateClientExists(String documentId) {
        log.info("ATTEMPT: Validating client exists with documentId: {}", documentId);

        String endpoint = AuthServiceEndpoints.getUserByDocumentUrl(authServiceBaseUrl);

        return webClient
                .get()
                .uri(endpoint, documentId)
                .retrieve()
                .bodyToMono(UserResponse.class)
                .map(user -> {
                    log.info("SUCCESS: Client validation successful for documentId: {}", documentId);
                    return true;
                })
                .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                    log.info("CLIENT NOT FOUND: documentId {} does not exist", documentId);
                    return Mono.error(new ClientNotFoundException("Client not found with documentId: " + documentId));
                })
                .doOnError(error -> log.error("RETRY: Error validating client with documentId {}: {}", documentId, error.getMessage()))
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorMap(ex -> {
                    if (ex instanceof ClientNotFoundException) {
                        return ex;
                    }
                    log.warn("FALLBACK: All retries failed for documentId: {}. Reason: {}", documentId, ex.getMessage());
                    return new ServiceUnavailableException("Authentication service is temporarily unavailable. Please try again later.", ex);
                });
    }
}
