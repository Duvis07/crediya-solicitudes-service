package co.com.crediya.solicitudes.config;

import co.com.crediya.solicitudes.model.client.gateways.ClientValidationRepository;
import co.com.crediya.solicitudes.webclient.AuthServiceClient;
import co.com.crediya.solicitudes.webclient.config.AuthServiceProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientAdapterConfig {

    private static final String AUTH_SERVICE_INSTANCE_NAME = "auth-service";

    @Bean
    public ClientValidationRepository clientValidationGateway(
            AuthServiceProperties authServiceProperties,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry) {
        
        WebClient webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
        
        Retry retry = retryRegistry.retry(AUTH_SERVICE_INSTANCE_NAME);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(AUTH_SERVICE_INSTANCE_NAME);
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter(AUTH_SERVICE_INSTANCE_NAME);
        
        return new AuthServiceClient(webClient, authServiceProperties, retry, circuitBreaker, timeLimiter);
    }
}
