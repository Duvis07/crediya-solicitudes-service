package co.com.crediya.solicitudes.aws.adapter;

import co.com.crediya.solicitudes.aws.dto.CapacityRequestDto;
import co.com.crediya.solicitudes.aws.sqs.MessageQueueService;
import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.application.gateways.ApplicationRepository;
import co.com.crediya.solicitudes.model.lambda.CapacityCalculationRequest;
import co.com.crediya.solicitudes.model.loantype.gateways.LoanTypeRepository;
import co.com.crediya.solicitudes.model.state.gateways.StateRepository;
import co.com.crediya.solicitudes.webclient.AuthServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutomaticEvaluationAdapterTest {

    @Mock
    private MessageQueueService messageQueueService;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private LoanTypeRepository loanTypeRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    private AutomaticEvaluationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AutomaticEvaluationAdapter(messageQueueService, applicationRepository, stateRepository, loanTypeRepository, authServiceClient);
    }

    @Test
    void sendForAutomaticEvaluationShouldSendMessageToQueueWhenValidRequest() {
        // Arrange
        Application application = Application.builder()
                .applicationId(1L)
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(24)
                .build();

        when(authServiceClient.getUserByDocumentId("12345678"))
                .thenReturn(Mono.just(co.com.crediya.solicitudes.webclient.dto.UserResponse.builder()
                        .firstName("Juan")
                        .lastName("Perez")
                        .email("test@example.com")
                        .build()));
        when(messageQueueService.sendApplicationForEvaluation(any(CapacityRequestDto.class)))
                .thenReturn(Mono.just("message-id-123"));

        // Act
        Mono<String> result = adapter.sendForAutomaticEvaluation(application);

        // Assert
        StepVerifier.create(result)
                .expectNext("message-id-123")
                .verifyComplete();

        verify(authServiceClient).getUserByDocumentId("12345678");
        verify(messageQueueService).sendApplicationForEvaluation(any(CapacityRequestDto.class));
    }

    @Test
    void sendForCapacityCalculation_ShouldSendMessageToQueue_WhenValidRequest() {
        // Arrange
        CapacityCalculationRequest request = CapacityCalculationRequest.builder()
                .documentoIdentidad("12345678")
                .email("test@example.com")
                .monto(new BigDecimal("500000"))
                .plazoMeses(24)
                .tasaInteresAnual(new BigDecimal("12.5"))
                .salarioBase(new BigDecimal("2000000"))
                .build();

        when(messageQueueService.sendApplicationForEvaluation(any(CapacityRequestDto.class)))
                .thenReturn(Mono.just("message-id-456"));

        // Act
        Mono<String> result = adapter.sendForCapacityCalculation(request);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(timestamp -> timestamp.matches("\\d+"))
                .verifyComplete();

        verify(messageQueueService).sendApplicationForEvaluation(any(CapacityRequestDto.class));
    }

    @Test
    void isAutomaticValidationEnabledShouldReturnTrueWhenLoanTypeExists() {
        // Arrange
        Long loanTypeId = 1L;

        when(loanTypeRepository.findById(loanTypeId))
                .thenReturn(Mono.just(co.com.crediya.solicitudes.model.loantype.LoanType.builder()
                        .loanTypeId(loanTypeId)
                        .name("Personal")
                        .build()));

        // Act
        Mono<Boolean> result = adapter.isAutomaticValidationEnabled(loanTypeId);

        // Assert
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();

        verify(loanTypeRepository).findById(loanTypeId);
    }

    @Test
    void isAutomaticValidationEnabledShouldReturnFalseWhenLoanTypeNotFound() {
        // Arrange
        Long loanTypeId = 999L;

        when(loanTypeRepository.findById(loanTypeId))
                .thenReturn(Mono.empty());

        // Act
        Mono<Boolean> result = adapter.isAutomaticValidationEnabled(loanTypeId);

        // Assert
        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();

        verify(loanTypeRepository).findById(loanTypeId);
    }


    @Test
    void sendForAutomaticEvaluation_ShouldHandleQueueError_WhenMessageQueueFails() {
        // Arrange
        Application application = Application.builder()
                .applicationId(1L)
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(24)
                .build();

        when(authServiceClient.getUserByDocumentId("12345678"))
                .thenReturn(Mono.error(new RuntimeException("Auth service error")));

        when(messageQueueService.sendApplicationForEvaluation(any(CapacityRequestDto.class)))
                .thenReturn(Mono.error(new RuntimeException("Queue error")));

        // Act
        Mono<String> result = adapter.sendForAutomaticEvaluation(application);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(authServiceClient).getUserByDocumentId("12345678");
    }
}
