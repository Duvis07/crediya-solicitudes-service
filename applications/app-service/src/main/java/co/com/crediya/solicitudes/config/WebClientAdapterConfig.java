package co.com.crediya.solicitudes.config;

import co.com.crediya.solicitudes.model.client.gateways.ClientValidationGateway;
import co.com.crediya.solicitudes.webclient.AuthServiceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientAdapterConfig {

    @Bean
    public ClientValidationGateway clientValidationGateway() {
        WebClient webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
        return new AuthServiceClient(webClient, "http://localhost:8080");
    }
}
