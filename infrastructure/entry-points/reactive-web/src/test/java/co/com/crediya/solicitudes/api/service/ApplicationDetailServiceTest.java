package co.com.crediya.solicitudes.api.service;

import co.com.crediya.solicitudes.api.dto.ApplicationDetailResponse;
import co.com.crediya.solicitudes.api.mapper.ApplicationDetailMapper;
import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.exceptions.UserServiceException;
import co.com.crediya.solicitudes.model.client.UserType;
import co.com.crediya.solicitudes.webclient.AuthServiceClient;
import co.com.crediya.solicitudes.webclient.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationDetailServiceTest {

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private ApplicationEnrichmentService enrichmentService;

    @Mock
    private ApplicationDetailMapper applicationDetailMapper;

    @InjectMocks
    private ApplicationDetailService applicationDetailService;

    private Application testApplication;
    private UserResponse testUser;
    private ApplicationDetailResponse testResponse;

    @BeforeEach
    void setUp() {
        testApplication = Application.builder()
                .applicationId(1L)
                .documentId("12345678")
                .stateId(1L)
                .loanTypeId(1L)
                .amount(BigDecimal.valueOf(100000))
                .term(12)
                .email("juan.perez@email.com")
                .build();

        testUser = UserResponse.builder()
                .id(1L)
                .firstName("Juan")
                .lastName("Pérez")
                .email("juan.perez@email.com")
                .baseSalary(50000.0)
                .userType(co.com.crediya.solicitudes.model.client.UserType.APPLICANT)
                .build();

        testResponse = ApplicationDetailResponse.builder()
                .amount(BigDecimal.valueOf(100000))
                .term(12)
                .email("juan.perez@email.com")
                .fullName("Juan Pérez")
                .loanTypeName("Personal")
                .interestRate(BigDecimal.valueOf(0.15))
                .applicationState("PENDING_REVIEW")
                .baseSalary(BigDecimal.valueOf(50000))
                .totalMonthlyDebt(BigDecimal.valueOf(1500))
                .build();
    }

    @Test
    void buildDetailResponseShouldReturnCompleteResponseWhenAllDataAvailable() {
        // Arrange
        when(authServiceClient.getUserByDocumentId("12345678"))
                .thenReturn(Mono.just(testUser));
        when(enrichmentService.getApplicationState(1L))
                .thenReturn(Mono.just("PENDING_REVIEW"));
        when(enrichmentService.getLoanTypeName(1L))
                .thenReturn(Mono.just("Personal"));
        when(enrichmentService.getLoanInterestRate(1L))
                .thenReturn(Mono.just(BigDecimal.valueOf(0.15)));
        when(enrichmentService.calculateTotalMonthlyDebt("12345678"))
                .thenReturn(Mono.just(BigDecimal.valueOf(1500)));
        when(applicationDetailMapper.toDetailResponse(
                eq(testApplication),
                eq(testUser),
                eq("PENDING_REVIEW"),
                eq("Personal"),
                eq(BigDecimal.valueOf(0.15)),
                eq(BigDecimal.valueOf(1500))
        )).thenReturn(testResponse);

        // Act
        Mono<ApplicationDetailResponse> result = applicationDetailService.buildDetailResponse(testApplication);

        // Assert
        StepVerifier.create(result)
                .expectNext(testResponse)
                .verifyComplete();

        verify(authServiceClient).getUserByDocumentId("12345678");
        verify(enrichmentService).getApplicationState(1L);
        verify(enrichmentService).getLoanTypeName(1L);
        verify(enrichmentService).getLoanInterestRate(1L);
        verify(enrichmentService).calculateTotalMonthlyDebt("12345678");
        verify(applicationDetailMapper).toDetailResponse(
                testApplication, testUser, "PENDING_REVIEW", "Personal",
                BigDecimal.valueOf(0.15), BigDecimal.valueOf(1500)
        );
    }

    @Test
    void buildDetailResponseShouldPropagateUserServiceExceptionWhenAuthServiceFails() {
        // Arrange
        RuntimeException originalError = new RuntimeException("Auth service unavailable");
        when(authServiceClient.getUserByDocumentId("12345678"))
                .thenReturn(Mono.error(originalError));

        // Act
        Mono<ApplicationDetailResponse> result = applicationDetailService.buildDetailResponse(testApplication);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(error ->
                        error instanceof UserServiceException &&
                                error.getMessage().contains("Failed to get user data for documentId: 12345678") &&
                                error.getCause() == originalError
                )
                .verify();

        verify(authServiceClient).getUserByDocumentId("12345678");
        verifyNoInteractions(enrichmentService, applicationDetailMapper);
    }

    @Test
    void buildDetailResponseShouldPropagateErrorWhenEnrichmentServiceFails() {
        // Arrange
        when(authServiceClient.getUserByDocumentId("12345678"))
                .thenReturn(Mono.just(testUser));
        when(enrichmentService.getApplicationState(1L))
                .thenReturn(Mono.error(new RuntimeException("State service error")));
        when(enrichmentService.getLoanTypeName(1L))
                .thenReturn(Mono.just("Personal"));
        when(enrichmentService.getLoanInterestRate(1L))
                .thenReturn(Mono.just(BigDecimal.valueOf(0.15)));
        when(enrichmentService.calculateTotalMonthlyDebt("12345678"))
                .thenReturn(Mono.just(BigDecimal.valueOf(1500)));

        // Act
        Mono<ApplicationDetailResponse> result = applicationDetailService.buildDetailResponse(testApplication);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(authServiceClient).getUserByDocumentId("12345678");
        verify(enrichmentService).getApplicationState(1L);
        verify(enrichmentService).getLoanTypeName(1L);
        verify(enrichmentService).getLoanInterestRate(1L);
        verify(enrichmentService).calculateTotalMonthlyDebt("12345678");
        verifyNoInteractions(applicationDetailMapper);
    }

    @Test
    void buildDetailResponseShouldHandleApplicationWithNullDocumentId() {
        // Arrange
        Application appWithNullDocumentId = Application.builder()
                .applicationId(1L)
                .documentId(null)
                .stateId(1L)
                .loanTypeId(1L)
                .build();

        when(authServiceClient.getUserByDocumentId(null))
                .thenReturn(Mono.error(new IllegalArgumentException("DocumentId cannot be null")));

        // Act
        Mono<ApplicationDetailResponse> result = applicationDetailService.buildDetailResponse(appWithNullDocumentId);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(error ->
                        error instanceof UserServiceException &&
                                error.getMessage().contains("Failed to get user data for documentId: null")
                )
                .verify();

        verify(authServiceClient).getUserByDocumentId(null);
        verifyNoInteractions(enrichmentService, applicationDetailMapper);
    }

    @Test
    void buildDetailResponseShouldHandlePartialEnrichmentData() {
        // Arrange
        when(authServiceClient.getUserByDocumentId("12345678"))
                .thenReturn(Mono.just(testUser));
        when(enrichmentService.getApplicationState(1L))
                .thenReturn(Mono.just("PENDING_REVIEW"));
        when(enrichmentService.getLoanTypeName(1L))
                .thenReturn(Mono.just("Personal"));
        when(enrichmentService.getLoanInterestRate(1L))
                .thenReturn(Mono.just(BigDecimal.ZERO)); // Edge case: zero interest rate
        when(enrichmentService.calculateTotalMonthlyDebt("12345678"))
                .thenReturn(Mono.just(BigDecimal.ZERO)); // Edge case: no existing debt

        ApplicationDetailResponse responseWithZeros = ApplicationDetailResponse.builder()
                .amount(BigDecimal.valueOf(100000))
                .term(12)
                .email("juan.perez@email.com")
                .fullName("Juan Pérez")
                .loanTypeName("Personal")
                .interestRate(BigDecimal.ZERO)
                .applicationState("PENDING_REVIEW")
                .baseSalary(BigDecimal.valueOf(50000))
                .totalMonthlyDebt(BigDecimal.ZERO)
                .build();

        when(applicationDetailMapper.toDetailResponse(
                eq(testApplication),
                eq(testUser),
                eq("PENDING_REVIEW"),
                eq("Personal"),
                eq(BigDecimal.ZERO),
                eq(BigDecimal.ZERO)
        )).thenReturn(responseWithZeros);

        // Act
        Mono<ApplicationDetailResponse> result = applicationDetailService.buildDetailResponse(testApplication);

        // Assert
        StepVerifier.create(result)
                .expectNext(responseWithZeros)
                .verifyComplete();

        verify(authServiceClient).getUserByDocumentId("12345678");
        verify(enrichmentService).getApplicationState(1L);
        verify(enrichmentService).getLoanTypeName(1L);
        verify(enrichmentService).getLoanInterestRate(1L);
        verify(enrichmentService).calculateTotalMonthlyDebt("12345678");
        verify(applicationDetailMapper).toDetailResponse(
                testApplication, testUser, "PENDING_REVIEW", "Personal",
                BigDecimal.ZERO, BigDecimal.ZERO
        );
    }

    @Test
    void buildDetailResponseShouldHandleEmptyUserResponse() {
        // Arrange
        UserResponse emptyUser = UserResponse.builder()
                .id(1L)
                .firstName("")
                .lastName("")
                .email("")
                .userType(UserType.APPLICANT)
                .build();

        when(authServiceClient.getUserByDocumentId("12345678"))
                .thenReturn(Mono.just(emptyUser));
        when(enrichmentService.getApplicationState(1L))
                .thenReturn(Mono.just("PENDING_REVIEW"));
        when(enrichmentService.getLoanTypeName(1L))
                .thenReturn(Mono.just("Personal"));
        when(enrichmentService.getLoanInterestRate(1L))
                .thenReturn(Mono.just(BigDecimal.valueOf(0.15)));
        when(enrichmentService.calculateTotalMonthlyDebt("12345678"))
                .thenReturn(Mono.just(BigDecimal.valueOf(1500)));

        ApplicationDetailResponse responseWithEmptyUser = ApplicationDetailResponse.builder()
                .amount(BigDecimal.valueOf(100000))
                .term(12)
                .email("")
                .fullName("")
                .loanTypeName("Personal")
                .interestRate(BigDecimal.valueOf(0.15))
                .applicationState("PENDING_REVIEW")
                .baseSalary(BigDecimal.valueOf(50000))
                .totalMonthlyDebt(BigDecimal.valueOf(1500))
                .build();

        when(applicationDetailMapper.toDetailResponse(
                eq(testApplication),
                eq(emptyUser),
                eq("PENDING_REVIEW"),
                eq("Personal"),
                eq(BigDecimal.valueOf(0.15)),
                eq(BigDecimal.valueOf(1500))
        )).thenReturn(responseWithEmptyUser);

        // Act
        Mono<ApplicationDetailResponse> result = applicationDetailService.buildDetailResponse(testApplication);

        // Assert
        StepVerifier.create(result)
                .expectNext(responseWithEmptyUser)
                .verifyComplete();

        verify(applicationDetailMapper).toDetailResponse(
                testApplication, emptyUser, "PENDING_REVIEW", "Personal",
                BigDecimal.valueOf(0.15), BigDecimal.valueOf(1500)
        );
    }
}
