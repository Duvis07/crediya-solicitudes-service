package co.com.crediya.solicitudes.config;

import co.com.crediya.solicitudes.model.client.gateways.ClientValidationRepository;
import co.com.crediya.solicitudes.webclient.AuthServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientAdapterConfig {

    @Value("${auth-service.base-url}")
    private String authServiceBaseUrl;

    @Bean
    public ClientValidationRepository clientValidationGateway() {
        WebClient webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
        return new AuthServiceClient(webClient, authServiceBaseUrl);
    }
}
