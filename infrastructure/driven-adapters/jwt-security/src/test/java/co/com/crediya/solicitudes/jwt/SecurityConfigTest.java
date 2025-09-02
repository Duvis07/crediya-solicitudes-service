package co.com.crediya.solicitudes.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private RoleAuthorizationFilter roleAuthorizationFilter;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(jwtAuthenticationFilter, roleAuthorizationFilter);
    }

    @Test
    void constructorShouldInitializeWithFilters() {
        // Act & Assert
        assertNotNull(securityConfig);
    }

    @Test
    void corsConfigurationSourceShouldReturnValidSource() {
        // Act
        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();

        // Assert
        assertNotNull(corsSource);
    }
}
