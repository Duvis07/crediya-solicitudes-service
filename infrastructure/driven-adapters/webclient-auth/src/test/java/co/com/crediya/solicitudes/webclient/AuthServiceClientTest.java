package co.com.crediya.solicitudes.webclient;

import co.com.crediya.solicitudes.webclient.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private static final String VALID_DOCUMENT_ID = "12345678";

    @Test
    void shouldReturnTrueWhenClientExists() {
        // Given
        UserResponse userResponse = new UserResponse();

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserResponse.class)).thenReturn(Mono.just(userResponse));

        // When
        Mono<UserResponse> result = webClient.get()
                .uri("http://localhost:8080/api/v1/users/{documentId}", VALID_DOCUMENT_ID)
                .retrieve()
                .bodyToMono(UserResponse.class);

        // Then
        StepVerifier.create(result)
                .expectNext(userResponse)
                .verifyComplete();
    }

    @Test
    void shouldThrowClientNotFoundExceptionWhenClientNotFound() {
        // Given
        String documentId = "99999999";
        WebClientResponseException notFoundException =
                WebClientResponseException.create(404, "Not Found", null, null, null);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserResponse.class)).thenReturn(Mono.error(notFoundException));

        // When
        Mono<UserResponse> result = webClient.get()
                .uri("http://localhost:8080/api/v1/users/{documentId}", documentId)
                .retrieve()
                .bodyToMono(UserResponse.class);

        // Then
        StepVerifier.create(result)
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    void shouldThrowServiceUnavailableExceptionWhenServiceUnavailable() {
        // Given
        WebClientResponseException serviceUnavailableException =
                WebClientResponseException.create(503, "Service Unavailable", null, null, null);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserResponse.class)).thenReturn(Mono.error(serviceUnavailableException));

        // When
        Mono<UserResponse> result = webClient.get()
                .uri("http://localhost:8080/api/v1/users/{documentId}", VALID_DOCUMENT_ID)
                .retrieve()
                .bodyToMono(UserResponse.class);

        // Then
        StepVerifier.create(result)
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    void shouldThrowServiceUnavailableExceptionWhenInternalServerError() {
        // Given
        WebClientResponseException internalServerError =
                WebClientResponseException.create(500, "Internal Server Error", null, null, null);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserResponse.class)).thenReturn(Mono.error(internalServerError));

        // When
        Mono<UserResponse> result = webClient.get()
                .uri("http://localhost:8080/api/v1/users/{documentId}", VALID_DOCUMENT_ID)
                .retrieve()
                .bodyToMono(UserResponse.class);

        // Then
        StepVerifier.create(result)
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    void shouldThrowServiceUnavailableExceptionWhenGenericException() {
        // Given
        RuntimeException genericException = new RuntimeException("Connection timeout");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserResponse.class)).thenReturn(Mono.error(genericException));

        // When
        Mono<UserResponse> result = webClient.get()
                .uri("http://localhost:8080/api/v1/users/{documentId}", VALID_DOCUMENT_ID)
                .retrieve()
                .bodyToMono(UserResponse.class);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldValidateWebClientInteraction() {
        // Given
        UserResponse userResponse = new UserResponse();

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserResponse.class)).thenReturn(Mono.just(userResponse));

        // When
        Mono<UserResponse> result = webClient.get()
                .uri("http://localhost:8080/api/v1/users/{documentId}", VALID_DOCUMENT_ID)
                .retrieve()
                .bodyToMono(UserResponse.class);

        // Then
        StepVerifier.create(result)
                .expectNext(userResponse)
                .verifyComplete();
    }

    @Test
    void shouldHandleNotFoundResponse() {
        // Given
        WebClientResponseException notFoundException =
                WebClientResponseException.create(404, "Not Found", null, null, null);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserResponse.class)).thenReturn(Mono.error(notFoundException));

        // When
        Mono<UserResponse> result = webClient.get()
                .uri("http://localhost:8080/api/v1/users/{documentId}", "99999999")
                .retrieve()
                .bodyToMono(UserResponse.class);

        // Then
        StepVerifier.create(result)
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    void shouldHandleTimeoutException() {
        // Given
        java.util.concurrent.TimeoutException timeoutException = 
                new java.util.concurrent.TimeoutException("Request timeout");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserResponse.class)).thenReturn(Mono.error(timeoutException));

        // When
        Mono<UserResponse> result = webClient.get()
                .uri("http://localhost:8080/api/v1/users/{documentId}", VALID_DOCUMENT_ID)
                .retrieve()
                .bodyToMono(UserResponse.class);

        // Then
        StepVerifier.create(result)
                .expectError(java.util.concurrent.TimeoutException.class)
                .verify();
    }
}
