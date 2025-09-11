package co.com.crediya.solicitudes.usecase.capacity;

import co.com.crediya.solicitudes.model.application.gateways.CapacityEvaluationRepository;
import co.com.crediya.solicitudes.model.client.gateways.ClientValidationRepository;
import co.com.crediya.solicitudes.model.lambda.CapacityCalculationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CapacityCalculationUseCaseTest {

    @Mock
    private CapacityEvaluationRepository capacityEvaluationRepository;

    @Mock
    private ClientValidationRepository clientValidationRepository;

    private CapacityCalculationUseCase capacityCalculationUseCase;

    @BeforeEach
    void setUp() {
        capacityCalculationUseCase = new CapacityCalculationUseCase(
                capacityEvaluationRepository,
                clientValidationRepository
        );
    }

    @Test
    void sendForCapacityCalculation_ShouldReturnMessageId_WhenValidRequest() {
        // Arrange
        CapacityCalculationRequest request = CapacityCalculationRequest.builder()
                .documentoIdentidad("12345678")
                .email("test@example.com")
                .monto(new BigDecimal("500000"))
                .plazoMeses(24)
                .tasaInteresAnual(new BigDecimal("12.5"))
                .salarioBase(new BigDecimal("3000000"))
                .build();

        String expectedMessageId = "message-123";
        String userEmail = "test@example.com";

        when(clientValidationRepository.getUserEmailByDocumentId("12345678"))
                .thenReturn(Mono.just(userEmail));
        when(capacityEvaluationRepository.sendForCapacityCalculation(request))
                .thenReturn(Mono.just(expectedMessageId));

        // Act & Assert
        StepVerifier.create(capacityCalculationUseCase.sendForCapacityCalculation(request))
                .expectNext(expectedMessageId)
                .verifyComplete();

        verify(clientValidationRepository).getUserEmailByDocumentId("12345678");
        verify(capacityEvaluationRepository).sendForCapacityCalculation(request);
    }

    @Test
    void sendForCapacityCalculation_ShouldHandleWhitespaceEmail_WhenEmailHasWhitespace() {
        // Arrange
        CapacityCalculationRequest request = CapacityCalculationRequest.builder()
                .documentoIdentidad("12345678")
                .email("  test@example.com  ")
                .monto(new BigDecimal("500000"))
                .plazoMeses(24)
                .tasaInteresAnual(new BigDecimal("12.5"))
                .salarioBase(new BigDecimal("3000000"))
                .build();

        String expectedMessageId = "message-123";
        String userEmail = "test@example.com";

        when(clientValidationRepository.getUserEmailByDocumentId("12345678"))
                .thenReturn(Mono.just(userEmail));
        when(capacityEvaluationRepository.sendForCapacityCalculation(request))
                .thenReturn(Mono.just(expectedMessageId));

        // Act & Assert
        StepVerifier.create(capacityCalculationUseCase.sendForCapacityCalculation(request))
                .expectNext(expectedMessageId)
                .verifyComplete();

        verify(clientValidationRepository).getUserEmailByDocumentId("12345678");
        verify(capacityEvaluationRepository).sendForCapacityCalculation(request);
    }

    @Test
    void sendForCapacityCalculation_ShouldPropagateRepositoryError_WhenRepositoryFails() {
        // Arrange
        CapacityCalculationRequest request = CapacityCalculationRequest.builder()
                .documentoIdentidad("12345678")
                .email("test@example.com")
                .monto(new BigDecimal("500000"))
                .plazoMeses(24)
                .tasaInteresAnual(new BigDecimal("12.5"))
                .salarioBase(new BigDecimal("3000000"))
                .build();

        String userEmail = "test@example.com";
        RuntimeException repositoryError = new RuntimeException("SQS connection failed");

        when(clientValidationRepository.getUserEmailByDocumentId("12345678"))
                .thenReturn(Mono.just(userEmail));
        when(capacityEvaluationRepository.sendForCapacityCalculation(request))
                .thenReturn(Mono.error(repositoryError));

        // Act & Assert
        StepVerifier.create(capacityCalculationUseCase.sendForCapacityCalculation(request))
                .expectError(RuntimeException.class)
                .verify();

        verify(clientValidationRepository).getUserEmailByDocumentId("12345678");
        verify(capacityEvaluationRepository).sendForCapacityCalculation(request);
    }
}
