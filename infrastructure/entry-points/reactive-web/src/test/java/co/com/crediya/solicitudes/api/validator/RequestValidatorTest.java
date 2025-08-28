package co.com.crediya.solicitudes.api.validator;

import co.com.crediya.solicitudes.api.dto.CreateApplicationRequest;
import co.com.crediya.solicitudes.api.exceptions.ValidationException;
import co.com.crediya.solicitudes.model.loantype.LoanTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestValidatorTest {

    @Mock
    private Validator validator;

    private RequestValidator requestValidator;

    @BeforeEach
    void setUp() {
        requestValidator = new RequestValidator(validator);
    }

    @Test
    void validate_ShouldReturnValidRequest_WhenNoValidationErrors() {
        // Arrange
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(24)
                .loanType(LoanTypeEnum.PERSONAL)
                .build();

        doNothing().when(validator).validate(eq(request), any(BeanPropertyBindingResult.class));

        // Act
        Mono<CreateApplicationRequest> result = requestValidator.validate(request, "createApplicationRequest");

        // Assert
        StepVerifier.create(result)
                .assertNext(validatedRequest -> {
                    assertNotNull(validatedRequest);
                    assertEquals("12345678", validatedRequest.getDocumentId());
                    assertEquals("test@example.com", validatedRequest.getEmail());
                    assertEquals(new BigDecimal("500000"), validatedRequest.getAmount());
                    assertEquals(24, validatedRequest.getTerm());
                    assertEquals(LoanTypeEnum.PERSONAL, validatedRequest.getLoanType());
                })
                .verifyComplete();

        verify(validator).validate(eq(request), any(BeanPropertyBindingResult.class));
    }

    @Test
    void validate_ShouldThrowValidationException_WhenValidationErrorsExist() {
        // Arrange
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .documentId("123") // Invalid: too short
                .email("invalid-email") // Invalid format
                .amount(new BigDecimal("50000")) // Invalid: too low
                .term(3) // Invalid: too short
                .loanType(LoanTypeEnum.PERSONAL)
                .build();

        doAnswer(invocation -> {
            BeanPropertyBindingResult bindingResult = invocation.getArgument(1);
            bindingResult.addError(new FieldError("createApplicationRequest", "documentId", "Document ID must be between 8 and 15 characters"));
            bindingResult.addError(new FieldError("createApplicationRequest", "email", "Email format is invalid"));
            bindingResult.addError(new FieldError("createApplicationRequest", "amount", "Amount must be at least 100,000"));
            bindingResult.addError(new FieldError("createApplicationRequest", "term", "Term must be at least 6 months"));
            return null;
        }).when(validator).validate(eq(request), any(BeanPropertyBindingResult.class));

        // Act
        Mono<CreateApplicationRequest> result = requestValidator.validate(request, "createApplicationRequest");

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(error -> {
                    assertInstanceOf(ValidationException.class, error);
                    ValidationException validationException = (ValidationException) error;
                    assertEquals("Validation failed", validationException.getMessage());
                    assertEquals(4, validationException.getErrors().getErrorCount());
                    return true;
                })
                .verify();

        verify(validator).validate(eq(request), any(BeanPropertyBindingResult.class));
    }

    static Stream<Arguments> invalidRequestData() {
        return Stream.of(
                Arguments.of(
                        CreateApplicationRequest.builder()
                                .documentId("1234567") // Too short
                                .email("test@example.com")
                                .amount(new BigDecimal("500000"))
                                .term(24)
                                .loanType(LoanTypeEnum.PERSONAL)
                                .build(),
                        "documentId"
                ),
                Arguments.of(
                        CreateApplicationRequest.builder()
                                .documentId("12345678901234567890") // Too long
                                .email("test@example.com")
                                .amount(new BigDecimal("500000"))
                                .term(24)
                                .loanType(LoanTypeEnum.PERSONAL)
                                .build(),
                        "documentId"
                ),
                Arguments.of(
                        CreateApplicationRequest.builder()
                                .documentId("12345678")
                                .email("invalid-email") // Invalid format
                                .amount(new BigDecimal("500000"))
                                .term(24)
                                .loanType(LoanTypeEnum.PERSONAL)
                                .build(),
                        "email"
                ),
                Arguments.of(
                        CreateApplicationRequest.builder()
                                .documentId("12345678")
                                .email("test@example.com")
                                .amount(new BigDecimal("50000")) // Too low
                                .term(24)
                                .loanType(LoanTypeEnum.PERSONAL)
                                .build(),
                        "amount"
                ),
                Arguments.of(
                        CreateApplicationRequest.builder()
                                .documentId("12345678")
                                .email("test@example.com")
                                .amount(new BigDecimal("60000000")) // Too high
                                .term(24)
                                .loanType(LoanTypeEnum.PERSONAL)
                                .build(),
                        "amount"
                ),
                Arguments.of(
                        CreateApplicationRequest.builder()
                                .documentId("12345678")
                                .email("test@example.com")
                                .amount(new BigDecimal("500000"))
                                .term(3) // Too short
                                .loanType(LoanTypeEnum.PERSONAL)
                                .build(),
                        "term"
                ),
                Arguments.of(
                        CreateApplicationRequest.builder()
                                .documentId("12345678")
                                .email("test@example.com")
                                .amount(new BigDecimal("500000"))
                                .term(72) // Too long
                                .loanType(LoanTypeEnum.PERSONAL)
                                .build(),
                        "term"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("invalidRequestData")
    void validate_ShouldThrowValidationException_WhenSpecificFieldIsInvalid(CreateApplicationRequest request, String expectedField) {
        // Arrange
        doAnswer(invocation -> {
            BeanPropertyBindingResult bindingResult = invocation.getArgument(1);
            bindingResult.addError(new FieldError("createApplicationRequest", expectedField, "Field validation failed"));
            return null;
        }).when(validator).validate(eq(request), any(BeanPropertyBindingResult.class));

        // Act
        Mono<CreateApplicationRequest> result = requestValidator.validate(request, "createApplicationRequest");

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(error -> {
                    assertInstanceOf(ValidationException.class, error);
                    ValidationException validationException = (ValidationException) error;
                    assertTrue(validationException.getErrors().hasFieldErrors(expectedField));
                    return true;
                })
                .verify();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void validate_ShouldThrowValidationException_WhenDocumentIdIsBlank(String documentId) {
        // Arrange
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .documentId(documentId)
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(24)
                .loanType(LoanTypeEnum.PERSONAL)
                .build();

        doAnswer(invocation -> {
            BeanPropertyBindingResult bindingResult = invocation.getArgument(1);
            bindingResult.addError(new FieldError("createApplicationRequest", "documentId", "Document ID is required"));
            return null;
        }).when(validator).validate(eq(request), any(BeanPropertyBindingResult.class));

        // Act
        Mono<CreateApplicationRequest> result = requestValidator.validate(request, "createApplicationRequest");

        // Assert
        StepVerifier.create(result)
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    void validate_ShouldHandleNullRequest_WhenRequestIsNull() {
        // Arrange
        CreateApplicationRequest request = null;

        doAnswer(invocation -> {
            BeanPropertyBindingResult bindingResult = invocation.getArgument(1);
            bindingResult.addError(new FieldError("createApplicationRequest", "request", "Request cannot be null"));
            return null;
        }).when(validator).validate(eq(request), any(BeanPropertyBindingResult.class));

        // Act
        Mono<CreateApplicationRequest> result = requestValidator.validate(request, "createApplicationRequest");

        // Assert
        StepVerifier.create(result)
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    void validate_ShouldUseCorrectObjectName_WhenValidating() {
        // Arrange
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(24)
                .loanType(LoanTypeEnum.PERSONAL)
                .build();

        String objectName = "testObjectName";
        doNothing().when(validator).validate(eq(request), any(BeanPropertyBindingResult.class));

        // Act
        Mono<CreateApplicationRequest> result = requestValidator.validate(request, objectName);

        // Assert
        StepVerifier.create(result)
                .expectNext(request)
                .verifyComplete();

        verify(validator).validate(eq(request), argThat(bindingResult -> 
                bindingResult.getObjectName().equals(objectName)));
    }
}
