package co.com.crediya.solicitudes.usecase.application;

import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.application.gateways.ApplicationRepository;
import co.com.crediya.solicitudes.model.exceptions.LoanTypeNotFoundException;
import co.com.crediya.solicitudes.model.loantype.LoanType;
import co.com.crediya.solicitudes.model.loantype.LoanTypeEnum;
import co.com.crediya.solicitudes.model.loantype.gateways.LoanTypeRepository;
import co.com.crediya.solicitudes.model.state.State;
import co.com.crediya.solicitudes.model.state.gateways.StateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ApplicationUseCaseTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private LoanTypeRepository loanTypeRepository;

    @Mock
    private StateRepository stateRepository;

    private ApplicationUseCase applicationUseCase;

    @BeforeEach
    void setUp() {
        applicationUseCase = new ApplicationUseCase(
                applicationRepository,
                loanTypeRepository,
                stateRepository
        );
    }

    @Test
    void createApplication_ShouldReturnApplication_WhenValidData() {
        // Arrange
        Application inputApplication = Application.builder()
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(12)
                .build();

        LoanType loanType = LoanType.builder()
                .loanTypeId(1L)
                .name("Prestamo Personal")
                .build();

        State state = State.builder()
                .stateId(1L)
                .name("Pendiente de revision")
                .build();

        Application savedApplication = Application.builder()
                .applicationId(1L)
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(12)
                .loanTypeId(1L)
                .stateId(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(loanTypeRepository.findByName("Prestamo Personal"))
                .thenReturn(Mono.just(loanType));
        when(stateRepository.findByName("Pendiente de revision"))
                .thenReturn(Mono.just(state));
        when(applicationRepository.save(any(Application.class)))
                .thenReturn(Mono.just(savedApplication));

        // Act
        Mono<Application> result = applicationUseCase.createApplication(
                inputApplication,
                LoanTypeEnum.PERSONAL
        );

        // Assert
        StepVerifier.create(result)
                .assertNext(application -> {
                    assertNotNull(application);
                    assertEquals(1L, application.getApplicationId());
                    assertEquals("12345678", application.getDocumentId());
                    assertEquals("test@example.com", application.getEmail());
                    assertEquals(new BigDecimal("500000"), application.getAmount());
                    assertEquals(12, application.getTerm());
                    assertEquals(1L, application.getLoanTypeId());
                    assertEquals(1L, application.getStateId());
                    assertNotNull(application.getCreatedAt());
                    assertNotNull(application.getUpdatedAt());
                })
                .verifyComplete();
    }

    @Test
    void createApplication_ShouldThrowException_WhenLoanTypeNotFound() {
        // Arrange
        Application inputApplication = Application.builder()
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(12)
                .build();

        State state = State.builder()
                .stateId(1L)
                .name("Pendiente de revision")
                .build();

        when(loanTypeRepository.findByName("Prestamo Personal"))
                .thenReturn(Mono.empty());
        when(stateRepository.findByName("Pendiente de revision"))
                .thenReturn(Mono.just(state));

        // Act
        Mono<Application> result = applicationUseCase.createApplication(
                inputApplication,
                LoanTypeEnum.PERSONAL
        );

        // Assert
        StepVerifier.create(result)
                .expectError(LoanTypeNotFoundException.class)
                .verify();
    }

    @Test
    void createApplication_ShouldThrowException_WhenStateNotFound() {
        // Arrange
        Application inputApplication = Application.builder()
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(12)
                .build();

        LoanType loanType = LoanType.builder()
                .loanTypeId(1L)
                .name("Prestamo Personal")
                .build();

        when(loanTypeRepository.findByName("Prestamo Personal"))
                .thenReturn(Mono.just(loanType));
        when(stateRepository.findByName("Pendiente de revision"))
                .thenReturn(Mono.empty());

        // Act
        Mono<Application> result = applicationUseCase.createApplication(
                inputApplication,
                LoanTypeEnum.PERSONAL
        );

        // Assert
        StepVerifier.create(result)
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void getAllApplications_ShouldReturnAllApplications() {
        // Arrange
        Application app1 = Application.builder()
                .applicationId(1L)
                .documentId("12345678")
                .email("test1@example.com")
                .amount(new BigDecimal("500000"))
                .term(12)
                .build();

        Application app2 = Application.builder()
                .applicationId(2L)
                .documentId("87654321")
                .email("test2@example.com")
                .amount(new BigDecimal("1000000"))
                .term(24)
                .build();

        when(applicationRepository.findAll())
                .thenReturn(Flux.just(app1, app2));

        // Act
        Flux<Application> result = applicationUseCase.getAllApplications();

        // Assert
        StepVerifier.create(result)
                .assertNext(application -> {
                    assertEquals(1L, application.getApplicationId());
                    assertEquals("12345678", application.getDocumentId());
                })
                .assertNext(application -> {
                    assertEquals(2L, application.getApplicationId());
                    assertEquals("87654321", application.getDocumentId());
                })
                .verifyComplete();
    }

    @Test
    void createApplication_ShouldSetTimestamps_WhenCreatingApplication() {
        // Arrange
        Application inputApplication = Application.builder()
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(12)
                .build();

        LoanType loanType = LoanType.builder()
                .loanTypeId(1L)
                .name("Prestamo Personal")
                .build();

        State state = State.builder()
                .stateId(1L)
                .name("Pendiente de revision")
                .build();

        when(loanTypeRepository.findByName("Prestamo Personal"))
                .thenReturn(Mono.just(loanType));
        when(stateRepository.findByName("Pendiente de revision"))
                .thenReturn(Mono.just(state));
        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> {
                    Application app = invocation.getArgument(0);
                    return Mono.just(app.toBuilder().applicationId(1L).build());
                });

        // Act
        Mono<Application> result = applicationUseCase.createApplication(
                inputApplication,
                LoanTypeEnum.PERSONAL
        );

        // Assert
        StepVerifier.create(result)
                .assertNext(application -> {
                    assertNotNull(application.getCreatedAt());
                    assertNotNull(application.getUpdatedAt());
                    assertEquals(application.getCreatedAt(), application.getUpdatedAt());
                })
                .verifyComplete();
    }
}
