package co.com.crediya.solicitudes.api.utils;

import co.com.crediya.solicitudes.model.application.Application;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LoanCalculationUtilsTest {

    @Test
    void calculateMonthlyPaymentShouldReturnCorrectValueWhenValidInput() {
        // Arrange
        Application application = Application.builder()
                .amount(new BigDecimal("120000.00"))
                .term(12)
                .build();

        // Act
        BigDecimal result = LoanCalculationUtils.calculateMonthlyPayment(application);

        // Assert
        assertEquals(new BigDecimal("10000.00"), result);
    }

    @Test
    void calculateMonthlyPaymentShouldRoundCorrectlyWhenDecimalResult() {
        // Arrange
        Application application = Application.builder()
                .amount(new BigDecimal("100000.00"))
                .term(7)
                .build();

        // Act
        BigDecimal result = LoanCalculationUtils.calculateMonthlyPayment(application);

        // Assert
        assertEquals(new BigDecimal("14285.71"), result);
    }

    @Test
    void calculateMonthlyPaymentShouldReturnZeroWhenAmountIsNull() {
        // Arrange
        Application application = Application.builder()
                .amount(null)
                .term(12)
                .build();

        // Act
        BigDecimal result = LoanCalculationUtils.calculateMonthlyPayment(application);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateMonthlyPaymentShouldReturnZeroWhenTermIsNull() {
        // Arrange
        Application application = Application.builder()
                .amount(new BigDecimal("100000.00"))
                .term(null)
                .build();

        // Act
        BigDecimal result = LoanCalculationUtils.calculateMonthlyPayment(application);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateMonthlyPaymentShouldReturnZeroWhenTermIsZero() {
        // Arrange
        Application application = Application.builder()
                .amount(new BigDecimal("100000.00"))
                .term(0)
                .build();

        // Act
        BigDecimal result = LoanCalculationUtils.calculateMonthlyPayment(application);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateMonthlyPaymentShouldReturnZeroWhenTermIsNegative() {
        // Arrange
        Application application = Application.builder()
                .amount(new BigDecimal("100000.00"))
                .term(-5)
                .build();

        // Act
        BigDecimal result = LoanCalculationUtils.calculateMonthlyPayment(application);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }

    @ParameterizedTest
    @MethodSource("addMonthlyPaymentsTestData")
    void addMonthlyPaymentsShouldReturnCorrectSum(BigDecimal payment1, BigDecimal payment2, BigDecimal expected) {
        // Act
        BigDecimal result = LoanCalculationUtils.addMonthlyPayments(payment1, payment2);

        // Assert
        assertEquals(expected, result);
    }

    static Stream<Arguments> addMonthlyPaymentsTestData() {
        return Stream.of(
                Arguments.of(new BigDecimal("1000.00"), new BigDecimal("2000.00"), new BigDecimal("3000.00")),
                Arguments.of(new BigDecimal("500.50"), new BigDecimal("300.25"), new BigDecimal("800.75")),
                Arguments.of(null, new BigDecimal("1000.00"), new BigDecimal("1000.00")),
                Arguments.of(new BigDecimal("1000.00"), null, new BigDecimal("1000.00")),
                Arguments.of(null, null, BigDecimal.ZERO),
                Arguments.of(BigDecimal.ZERO, new BigDecimal("500.00"), new BigDecimal("500.00")),
                Arguments.of(new BigDecimal("100.33"), BigDecimal.ZERO, new BigDecimal("100.33"))
        );
    }
}
