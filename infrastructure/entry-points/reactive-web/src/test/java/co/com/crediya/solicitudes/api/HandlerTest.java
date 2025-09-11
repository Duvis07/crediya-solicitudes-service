package co.com.crediya.solicitudes.api;

import co.com.crediya.solicitudes.api.dto.ApplicationResponse;
import co.com.crediya.solicitudes.api.dto.CreateApplicationRequest;
import co.com.crediya.solicitudes.api.exceptions.ValidationException;
import co.com.crediya.solicitudes.api.mapper.ApplicationDtoMapper;
import co.com.crediya.solicitudes.api.mapper.PageResponseMapper;
import co.com.crediya.solicitudes.api.validator.RequestValidator;
import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.common.PageRequest;
import co.com.crediya.solicitudes.model.common.PageResponse;
import co.com.crediya.solicitudes.model.loantype.LoanTypeEnum;
import co.com.crediya.solicitudes.usecase.application.ApplicationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandlerTest {

    @Mock
    private ApplicationUseCase applicationUseCase;

    @Mock
    private ApplicationDtoMapper applicationDtoMapper;

    @Mock
    private PageResponseMapper pageResponseMapper;

    @Mock
    private RequestValidator requestValidator;

    private Handler handler;

    @BeforeEach
    void setUp() {
        handler = new Handler(applicationUseCase, applicationDtoMapper, pageResponseMapper, requestValidator);
    }

    static Stream<Arguments> validApplicationData() {
        return Stream.of(
                Arguments.of(
                        CreateApplicationRequest.builder()
                                .documentId("12345678")
                                .email("test@example.com")
                                .amount(new BigDecimal("500000"))
                                .term(24)
                                .loanType(LoanTypeEnum.PERSONAL)
                                .build(),
                        Application.builder()
                                .documentId("12345678")
                                .email("test@example.com")
                                .amount(new BigDecimal("500000"))
                                .term(24)
                                .build()
                ),
                Arguments.of(
                        CreateApplicationRequest.builder()
                                .documentId("87654321")
                                .email("mortgage@example.com")
                                .amount(new BigDecimal("25000000"))
                                .term(36)
                                .loanType(LoanTypeEnum.MORTGAGE)
                                .build(),
                        Application.builder()
                                .documentId("87654321")
                                .email("mortgage@example.com")
                                .amount(new BigDecimal("25000000"))
                                .term(36)
                                .build()
                )
        );
    }

    @ParameterizedTest
    @MethodSource("validApplicationData")
    void createApplicationShouldReturnSuccessResponseWhenValidRequest(CreateApplicationRequest request, Application expectedApplication) {
        // Arrange
        ServerRequest serverRequest = MockServerRequest.builder()
                .body(Mono.just(request));

        Application createdApplication = expectedApplication.toBuilder()
                .applicationId(1L)
                .stateId(1L)
                .loanTypeId(1L)
                .createdAt(LocalDateTime.now())
                .build();

        when(requestValidator.validate(request, "createApplicationRequest"))
                .thenReturn(Mono.just(request));
        when(applicationDtoMapper.toDomain(request))
                .thenReturn(expectedApplication);
        when(applicationUseCase.createApplication(expectedApplication, request.getLoanType()))
                .thenReturn(Mono.just(createdApplication));

        // Act
        Mono<ServerResponse> result = handler.createApplication(serverRequest);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(HttpStatus.OK, response.statusCode());
                })
                .verifyComplete();

        verify(requestValidator).validate(request, "createApplicationRequest");
        verify(applicationDtoMapper).toDomain(request);
        verify(applicationUseCase).createApplication(expectedApplication, request.getLoanType());
    }

    @Test
    void createApplicationShouldReturnErrorWhenValidationFails() {
        // Arrange
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .documentId("123") // Invalid
                .email("invalid-email")
                .amount(new BigDecimal("50000"))
                .term(3)
                .loanType(LoanTypeEnum.PERSONAL)
                .build();

        ServerRequest serverRequest = MockServerRequest.builder()
                .body(Mono.just(request));

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "createApplicationRequest");
        ValidationException validationException = new ValidationException("Validation failed", bindingResult);

        when(requestValidator.validate((request), ("createApplicationRequest")))
                .thenReturn(Mono.error(validationException));

        // Act
        Mono<ServerResponse> result = handler.createApplication(serverRequest);

        // Assert
        StepVerifier.create(result)
                .expectError(ValidationException.class)
                .verify();

        verify(requestValidator).validate((request), ("createApplicationRequest"));
        verifyNoInteractions(applicationDtoMapper);
        verifyNoInteractions(applicationUseCase);
    }

    @Test
    void createApplicationShouldReturnErrorWhenUseCaseFails() {
        // Arrange
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(24)
                .loanType(LoanTypeEnum.PERSONAL)
                .build();

        Application application = Application.builder()
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(24)
                .build();

        ServerRequest serverRequest = MockServerRequest.builder()
                .body(Mono.just(request));

        RuntimeException useCaseException = new RuntimeException("Database error");

        when(requestValidator.validate((request), ("createApplicationRequest")))
                .thenReturn(Mono.just(request));
        when(applicationDtoMapper.toDomain(request))
                .thenReturn(application);
        when(applicationUseCase.createApplication((application), (request.getLoanType())))
                .thenReturn(Mono.error(useCaseException));

        // Act
        Mono<ServerResponse> result = handler.createApplication(serverRequest);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(requestValidator).validate((request), ("createApplicationRequest"));
        verify(applicationDtoMapper).toDomain(request);
        verify(applicationUseCase).createApplication((application), (request.getLoanType()));
    }

    @Test
    void getAllApplicationsShouldReturnApplicationsListWhenApplicationsExist() {
        // Arrange
        Application app1 = Application.builder()
                .applicationId(1L)
                .documentId("12345678")
                .email("test1@example.com")
                .amount(new BigDecimal("500000"))
                .term(24)
                .stateId(1L)
                .loanTypeId(1L)
                .createdAt(LocalDateTime.now())
                .build();

        Application app2 = Application.builder()
                .applicationId(2L)
                .documentId("87654321")
                .email("test2@example.com")
                .amount(new BigDecimal("1000000"))
                .term(36)
                .stateId(1L)
                .loanTypeId(2L)
                .createdAt(LocalDateTime.now())
                .build();

        ApplicationResponse response1 = ApplicationResponse.builder()
                .applicationId(1L)
                .documentId("12345678")
                .email("test1@example.com")
                .amount(new BigDecimal("500000"))
                .term(24)
                .build();

        ApplicationResponse response2 = ApplicationResponse.builder()
                .applicationId(2L)
                .documentId("87654321")
                .email("test2@example.com")
                .amount(new BigDecimal("1000000"))
                .term(36)
                .build();

        ServerRequest serverRequest = MockServerRequest.builder().build();

        PageRequest pageRequest = PageRequest.of(0, 10, "createdAt", "desc");
        PageResponse<Application> pageResponse = PageResponse.of(List.of(app1, app2), pageRequest, 2L);
        Map<String, Object> expectedResponse = Map.of(
            "content", List.of(response1, response2),
            "totalElements", 2L,
            "totalPages", 1,
            "currentPage", 0,
            "pageSize", 10
        );

        when(applicationUseCase.getApplicationsForManualReviewPaginated(any(PageRequest.class)))
                .thenReturn(Mono.just(pageResponse));
        when(pageResponseMapper.buildPageResponseWithDetails(pageResponse))
                .thenReturn(Mono.just(expectedResponse));

        // Act
        Mono<ServerResponse> result = handler.getAllApplications(serverRequest);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(HttpStatus.OK, response.statusCode());
                })
                .verifyComplete();

        verify(applicationUseCase).getApplicationsForManualReviewPaginated(any(PageRequest.class));
        verify(pageResponseMapper).buildPageResponseWithDetails(pageResponse);
    }

    @Test
    void getAllApplicationsShouldReturnEmptyListWhenNoApplicationsExist() {
        // Arrange
        ServerRequest serverRequest = MockServerRequest.builder().build();

        PageRequest pageRequest = PageRequest.of(0, 10, "createdAt", "desc");
        PageResponse<Application> emptyPageResponse = PageResponse.of(List.of(), pageRequest, 0L);
        Map<String, Object> emptyResponse = Map.of(
            "content", List.of(),
            "totalElements", 0L,
            "totalPages", 0,
            "currentPage", 0,
            "pageSize", 10
        );

        when(applicationUseCase.getApplicationsForManualReviewPaginated(any(PageRequest.class)))
                .thenReturn(Mono.just(emptyPageResponse));
        when(pageResponseMapper.buildPageResponseWithDetails(emptyPageResponse))
                .thenReturn(Mono.just(emptyResponse));

        // Act
        Mono<ServerResponse> result = handler.getAllApplications(serverRequest);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(HttpStatus.OK, response.statusCode());
                })
                .verifyComplete();

        verify(applicationUseCase).getApplicationsForManualReviewPaginated(any(PageRequest.class));
        verify(pageResponseMapper).buildPageResponseWithDetails(emptyPageResponse);
    }

    @Test
    void getAllApplicationsShouldReturnInternalServerErrorWhenUseCaseFails() {
        // Arrange
        ServerRequest serverRequest = MockServerRequest.builder().build();
        RuntimeException useCaseException = new RuntimeException("Database connection failed");

        when(applicationUseCase.getApplicationsForManualReviewPaginated(any(PageRequest.class)))
                .thenReturn(Mono.error(useCaseException));

        // Act
        Mono<ServerResponse> result = handler.getAllApplications(serverRequest);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(applicationUseCase).getApplicationsForManualReviewPaginated(any(PageRequest.class));
    }

    @Test
    void createApplicationShouldHandleMapperErrorWhenMappingFails() {
        // Arrange
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(24)
                .loanType(LoanTypeEnum.PERSONAL)
                .build();

        ServerRequest serverRequest = MockServerRequest.builder()
                .body(Mono.just(request));

        RuntimeException mapperException = new RuntimeException("Mapping failed");

        when(requestValidator.validate((request), ("createApplicationRequest")))
                .thenReturn(Mono.just(request));
        when(applicationDtoMapper.toDomain(request))
                .thenThrow(mapperException);

        // Act
        Mono<ServerResponse> result = handler.createApplication(serverRequest);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(requestValidator).validate((request), ("createApplicationRequest"));
        verify(applicationDtoMapper).toDomain(request);
        verifyNoInteractions(applicationUseCase);
    }
}
