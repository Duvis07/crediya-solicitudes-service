package co.com.crediya.solicitudes.webclient;

import co.com.crediya.solicitudes.model.client.gateways.ClientValidationRepository;
import co.com.crediya.solicitudes.webclient.config.AuthServiceEndpoints;
import co.com.crediya.solicitudes.webclient.dto.UserResponse;
import co.com.crediya.solicitudes.webclient.util.AuthServiceUtils;
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
    public Mono<String> getUserEmailByDocumentId(String documentId) {
        log.info("Getting user email for documentId: {}", documentId);

        return getUserFromAuthService(documentId)
                .flatMap(user -> AuthServiceUtils.validateApplicantRole(user, documentId))
                .map(UserResponse::getEmail)
                .doOnSuccess(email -> log.info("User email retrieved for documentId: {} - email: {}",
                        documentId, email));
    }

    private Mono<UserResponse> getUserFromAuthService(String documentId) {
        String endpoint = AuthServiceEndpoints.getUserByDocumentUrl(authServiceBaseUrl);

        return webClient
                .get()
                .uri(endpoint, documentId)
                .retrieve()
                .bodyToMono(UserResponse.class)
                .onErrorResume(WebClientResponseException.NotFound.class,
                        ex -> AuthServiceUtils.handleNotFoundError(documentId))
                .doOnError(error -> log.error("RETRY: Error calling auth service for documentId {}: {}",
                        documentId, error.getMessage()))
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorMap(ex -> AuthServiceUtils.mapToBusinessException(ex, documentId));
    }
}
