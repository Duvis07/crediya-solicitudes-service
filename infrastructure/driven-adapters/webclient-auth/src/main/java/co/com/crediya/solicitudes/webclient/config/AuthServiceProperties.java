package co.com.crediya.solicitudes.webclient.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "auth-service")
public class AuthServiceProperties {
    
    private String baseUrl;
    private Endpoints endpoints = new Endpoints();
    
    @Data
    public static class Endpoints {
        private String apiBase;
        private String usersBase;
        private String userByDocument;
    }
    
    public String getUserByDocumentUrl() {
        return baseUrl + 
               (endpoints.getApiBase() != null ? endpoints.getApiBase() : "") + 
               (endpoints.getUsersBase() != null ? endpoints.getUsersBase() : "") + 
               (endpoints.getUserByDocument() != null ? endpoints.getUserByDocument() : "");
    }
}
