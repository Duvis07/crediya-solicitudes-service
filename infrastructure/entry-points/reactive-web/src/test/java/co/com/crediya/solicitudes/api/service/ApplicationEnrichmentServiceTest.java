package co.com.crediya.solicitudes.api.service;

import co.com.crediya.solicitudes.api.utils.LoanCalculationUtils;
import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.application.gateways.ApplicationRepository;
import co.com.crediya.solicitudes.model.loantype.LoanType;
import co.com.crediya.solicitudes.model.loantype.gateways.LoanTypeRepository;
import co.com.crediya.solicitudes.model.state.ApplicationStatus;
import co.com.crediya.solicitudes.model.state.State;
import co.com.crediya.solicitudes.model.state.gateways.StateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationEnrichmentServiceTest {

    @Mock
    private StateRepository stateRepository;

    @Mock
    private LoanTypeRepository loanTypeRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private ApplicationEnrichmentService enrichmentService;

    private State testState;
    private LoanType testLoanType;
    private Application testApplication1;
    private Application testApplication2;

    @BeforeEach
    void setUp() {
        testState = State.builder()
                .stateId(1L)
                .name("PENDING_REVIEW")
                .build();

        testLoanType = LoanType.builder()
                .loanTypeId(1L)
                .name("Personal")
                .interestRate(BigDecimal.valueOf(0.15))
                .build();

        testApplication1 = Application.builder()
                .applicationId(1L)
                .documentId("12345678")
                .amount(BigDecimal.valueOf(100000))
                .term(12)
                .stateId(2L) // DISBURSED state
                .loanTypeId(1L)
                .build();

        testApplication2 = Application.builder()
                .applicationId(2L)
                .documentId("12345678")
                .amount(BigDecimal.valueOf(50000))
                .term(24)
                .stateId(2L) // DISBURSED state
                .loanTypeId(1L)
                .build();
    }

    @Test
    void getApplicationStateShouldReturnStateNameWhenStateExists() {
        // Arrange
        when(stateRepository.findById(1L)).thenReturn(Mono.just(testState));

        // Act
        Mono<String> result = enrichmentService.getApplicationState(1L);

        // Assert
        StepVerifier.create(result)
                .expectNext("PENDING_REVIEW")
                .verifyComplete();

        verify(stateRepository).findById(1L);
    }

    @Test
    void getApplicationStateShouldPropagateErrorWhenStateNotFound() {
        // Arrange
        when(stateRepository.findById(999L))
                .thenReturn(Mono.error(new RuntimeException("State not found")));

        // Act
        Mono<String> result = enrichmentService.getApplicationState(999L);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(stateRepository).findById(999L);
    }

    @Test
    void getLoanTypeNameShouldReturnLoanTypeNameWhenExists() {
        // Arrange
        when(loanTypeRepository.findById(1L)).thenReturn(Mono.just(testLoanType));

        // Act
        Mono<String> result = enrichmentService.getLoanTypeName(1L);

        // Assert
        StepVerifier.create(result)
                .expectNext("Personal")
                .verifyComplete();

        verify(loanTypeRepository).findById(1L);
    }

    @Test
    void getLoanTypeNameShouldPropagateErrorWhenLoanTypeNotFound() {
        // Arrange
        when(loanTypeRepository.findById(999L))
                .thenReturn(Mono.error(new RuntimeException("LoanType not found")));

        // Act
        Mono<String> result = enrichmentService.getLoanTypeName(999L);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(loanTypeRepository).findById(999L);
    }

    @Test
    void getLoanInterestRateShouldReturnInterestRateWhenExists() {
        // Arrange
        when(loanTypeRepository.findById(1L)).thenReturn(Mono.just(testLoanType));

        // Act
        Mono<BigDecimal> result = enrichmentService.getLoanInterestRate(1L);

        // Assert
        StepVerifier.create(result)
                .expectNext(BigDecimal.valueOf(0.15))
                .verifyComplete();

        verify(loanTypeRepository).findById(1L);
    }

    @Test
    void getLoanInterestRateShouldPropagateErrorWhenLoanTypeNotFound() {
        // Arrange
        when(loanTypeRepository.findById(999L))
                .thenReturn(Mono.error(new RuntimeException("LoanType not found")));

        // Act
        Mono<BigDecimal> result = enrichmentService.getLoanInterestRate(999L);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(loanTypeRepository).findById(999L);
    }

    @Test
    void calculateTotalMonthlyDebtShouldReturnSumWhenApplicationsExist() {
        // Arrange
        State disbursedState = State.builder()
                .stateId(2L)
                .name("DISBURSED")
                .build();

        when(stateRepository.findByName(ApplicationStatus.DISBURSED.getDescription()))
                .thenReturn(Mono.just(disbursedState));
        when(applicationRepository.findByDocumentIdAndStateId("12345678", 2L))
                .thenReturn(Flux.just(testApplication1, testApplication2));

        try (MockedStatic<LoanCalculationUtils> mockedUtils = mockStatic(LoanCalculationUtils.class)) {
            mockedUtils.when(() -> LoanCalculationUtils.calculateMonthlyPayment(testApplication1))
                    .thenReturn(BigDecimal.valueOf(9000));
            mockedUtils.when(() -> LoanCalculationUtils.calculateMonthlyPayment(testApplication2))
                    .thenReturn(BigDecimal.valueOf(2500));
            mockedUtils.when(() -> LoanCalculationUtils.addMonthlyPayments(any(), any()))
                    .thenCallRealMethod();

            // Act
            Mono<BigDecimal> result = enrichmentService.calculateTotalMonthlyDebt("12345678");

            // Assert
            StepVerifier.create(result)
                    .expectNext(BigDecimal.valueOf(11500))
                    .verifyComplete();
        }

        verify(stateRepository).findByName(ApplicationStatus.DISBURSED.getDescription());
        verify(applicationRepository).findByDocumentIdAndStateId("12345678", 2L);
    }

    @Test
    void calculateTotalMonthlyDebtShouldReturnZeroWhenNoApplicationsExist() {
        // Arrange
        State disbursedState = State.builder()
                .stateId(2L)
                .name("DISBURSED")
                .build();

        when(stateRepository.findByName(ApplicationStatus.DISBURSED.getDescription()))
                .thenReturn(Mono.just(disbursedState));
        when(applicationRepository.findByDocumentIdAndStateId("12345678", 2L))
                .thenReturn(Flux.empty());

        // Act
        Mono<BigDecimal> result = enrichmentService.calculateTotalMonthlyDebt("12345678");

        // Assert
        StepVerifier.create(result)
                .expectNext(BigDecimal.ZERO)
                .verifyComplete();

        verify(stateRepository).findByName(ApplicationStatus.DISBURSED.getDescription());
        verify(applicationRepository).findByDocumentIdAndStateId("12345678", 2L);
    }

    @Test
    void calculateTotalMonthlyDebtShouldReturnZeroWhenStateNotFound() {
        // Arrange
        when(stateRepository.findByName(ApplicationStatus.DISBURSED.getDescription()))
                .thenReturn(Mono.error(new RuntimeException("State not found")));

        // Act
        Mono<BigDecimal> result = enrichmentService.calculateTotalMonthlyDebt("12345678");

        // Assert
        StepVerifier.create(result)
                .expectNext(BigDecimal.ZERO)
                .verifyComplete();

        verify(stateRepository).findByName(ApplicationStatus.DISBURSED.getDescription());
        verifyNoInteractions(applicationRepository);
    }

    @Test
    void calculateTotalMonthlyDebtShouldReturnZeroWhenApplicationRepositoryFails() {
        // Arrange
        State disbursedState = State.builder()
                .stateId(2L)
                .name("DISBURSED")
                .build();

        when(stateRepository.findByName(ApplicationStatus.DISBURSED.getDescription()))
                .thenReturn(Mono.just(disbursedState));
        when(applicationRepository.findByDocumentIdAndStateId("12345678", 2L))
                .thenReturn(Flux.error(new RuntimeException("Database error")));

        // Act
        Mono<BigDecimal> result = enrichmentService.calculateTotalMonthlyDebt("12345678");

        // Assert
        StepVerifier.create(result)
                .expectNext(BigDecimal.ZERO)
                .verifyComplete();

        verify(stateRepository).findByName(ApplicationStatus.DISBURSED.getDescription());
        verify(applicationRepository).findByDocumentIdAndStateId("12345678", 2L);
    }

    @Test
    void calculateTotalMonthlyDebtShouldHandleSingleApplication() {
        // Arrange
        State disbursedState = State.builder()
                .stateId(2L)
                .name("DISBURSED")
                .build();

        when(stateRepository.findByName(ApplicationStatus.DISBURSED.getDescription()))
                .thenReturn(Mono.just(disbursedState));
        when(applicationRepository.findByDocumentIdAndStateId("12345678", 2L))
                .thenReturn(Flux.just(testApplication1));

        try (MockedStatic<LoanCalculationUtils> mockedUtils = mockStatic(LoanCalculationUtils.class)) {
            mockedUtils.when(() -> LoanCalculationUtils.calculateMonthlyPayment(testApplication1))
                    .thenReturn(BigDecimal.valueOf(9000));
            mockedUtils.when(() -> LoanCalculationUtils.addMonthlyPayments(any(), any()))
                    .thenCallRealMethod();

            // Act
            Mono<BigDecimal> result = enrichmentService.calculateTotalMonthlyDebt("12345678");

            // Assert
            StepVerifier.create(result)
                    .expectNext(BigDecimal.valueOf(9000))
                    .verifyComplete();
        }

        verify(stateRepository).findByName(ApplicationStatus.DISBURSED.getDescription());
        verify(applicationRepository).findByDocumentIdAndStateId("12345678", 2L);
    }

    @Test
    void calculateTotalMonthlyDebtShouldHandleZeroPayments() {
        // Arrange
        State disbursedState = State.builder()
                .stateId(2L)
                .name("DISBURSED")
                .build();

        when(stateRepository.findByName(ApplicationStatus.DISBURSED.getDescription()))
                .thenReturn(Mono.just(disbursedState));
        when(applicationRepository.findByDocumentIdAndStateId("12345678", 2L))
                .thenReturn(Flux.just(testApplication1, testApplication2));

        try (MockedStatic<LoanCalculationUtils> mockedUtils = mockStatic(LoanCalculationUtils.class)) {
            mockedUtils.when(() -> LoanCalculationUtils.calculateMonthlyPayment(any()))
                    .thenReturn(BigDecimal.ZERO);
            mockedUtils.when(() -> LoanCalculationUtils.addMonthlyPayments(any(), any()))
                    .thenCallRealMethod();

            // Act
            Mono<BigDecimal> result = enrichmentService.calculateTotalMonthlyDebt("12345678");

            // Assert
            StepVerifier.create(result)
                    .expectNext(BigDecimal.ZERO)
                    .verifyComplete();
        }

        verify(stateRepository).findByName(ApplicationStatus.DISBURSED.getDescription());
        verify(applicationRepository).findByDocumentIdAndStateId("12345678", 2L);
    }

    @Test
    void calculateTotalMonthlyDebtShouldHandleNullDocumentId() {
        // Arrange
        State disbursedState = State.builder()
                .stateId(2L)
                .name("DISBURSED")
                .build();

        when(stateRepository.findByName(ApplicationStatus.DISBURSED.getDescription()))
                .thenReturn(Mono.just(disbursedState));
        when(applicationRepository.findByDocumentIdAndStateId(null, 2L))
                .thenReturn(Flux.error(new IllegalArgumentException("DocumentId cannot be null")));

        // Act
        Mono<BigDecimal> result = enrichmentService.calculateTotalMonthlyDebt(null);

        // Assert
        StepVerifier.create(result)
                .expectNext(BigDecimal.ZERO)
                .verifyComplete();

        verify(stateRepository).findByName(ApplicationStatus.DISBURSED.getDescription());
        verify(applicationRepository).findByDocumentIdAndStateId(null, 2L);
    }
}
