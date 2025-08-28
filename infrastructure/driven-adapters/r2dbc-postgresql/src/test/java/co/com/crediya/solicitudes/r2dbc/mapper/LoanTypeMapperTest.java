package co.com.crediya.solicitudes.r2dbc.mapper;

import co.com.crediya.solicitudes.model.loantype.LoanType;
import co.com.crediya.solicitudes.r2dbc.entity.LoanTypeEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {LoanTypeMapperImpl.class})
class LoanTypeMapperTest {

    @Autowired
    private LoanTypeMapper loanTypeMapper;

    static Stream<Arguments> loanTypeTestData() {
        return Stream.of(
                Arguments.of(
                        LoanTypeEntity.builder()
                                .loanTypeId(1L)
                                .name("PERSONAL")
                                .interestRate(new BigDecimal("15.5"))
                                .minimumAmount(new BigDecimal("100000"))
                                .maxAmount(new BigDecimal("5000000"))
                                .automaticValidation(true)
                                .build(),
                        LoanType.builder()
                                .loanTypeId(1L)
                                .name("PERSONAL")
                                .interestRate(new BigDecimal("15.5"))
                                .minimumAmount(new BigDecimal("100000"))
                                .maxAmount(new BigDecimal("5000000"))
                                .automaticValidation(true)
                                .build()
                ),
                Arguments.of(
                        LoanTypeEntity.builder()
                                .loanTypeId(2L)
                                .name("MORTGAGE")
                                .interestRate(new BigDecimal("12.0"))
                                .minimumAmount(new BigDecimal("10000000"))
                                .maxAmount(new BigDecimal("50000000"))
                                .automaticValidation(false)
                                .build(),
                        LoanType.builder()
                                .loanTypeId(2L)
                                .name("MORTGAGE")
                                .interestRate(new BigDecimal("12.0"))
                                .minimumAmount(new BigDecimal("10000000"))
                                .maxAmount(new BigDecimal("50000000"))
                                .automaticValidation(false)
                                .build()
                ),
                Arguments.of(
                        LoanTypeEntity.builder()
                                .loanTypeId(3L)
                                .name("VEHICLE")
                                .interestRate(new BigDecimal("18.0"))
                                .minimumAmount(new BigDecimal("500000"))
                                .maxAmount(new BigDecimal("15000000"))
                                .automaticValidation(true)
                                .build(),
                        LoanType.builder()
                                .loanTypeId(3L)
                                .name("VEHICLE")
                                .interestRate(new BigDecimal("18.0"))
                                .minimumAmount(new BigDecimal("500000"))
                                .maxAmount(new BigDecimal("15000000"))
                                .automaticValidation(true)
                                .build()
                )
        );
    }

    @ParameterizedTest
    @MethodSource("loanTypeTestData")
    void toDomain_ShouldMapEntityToLoanType_WhenValidEntity(LoanTypeEntity entity, LoanType expectedLoanType) {
        // Act
        LoanType result = loanTypeMapper.toDomain(entity);

        // Assert
        assertNotNull(result);
        assertEquals(expectedLoanType.getLoanTypeId(), result.getLoanTypeId());
        assertEquals(expectedLoanType.getName(), result.getName());
        assertEquals(expectedLoanType.getInterestRate(), result.getInterestRate());
        assertEquals(expectedLoanType.getMinimumAmount(), result.getMinimumAmount());
        assertEquals(expectedLoanType.getMaxAmount(), result.getMaxAmount());
        assertEquals(expectedLoanType.getAutomaticValidation(), result.getAutomaticValidation());
    }

    @ParameterizedTest
    @NullSource
    void toDomain_ShouldReturnNull_WhenEntityIsNull(LoanTypeEntity entity) {
        // Act
        LoanType result = loanTypeMapper.toDomain(entity);

        // Assert
        assertNull(result);
    }

    @Test
    void toDomain_ShouldMapPartialData_WhenSomeFieldsAreNull() {
        // Arrange
        LoanTypeEntity entity = LoanTypeEntity.builder()
                .loanTypeId(1L)
                .name("PERSONAL")
                .build();

        // Act
        LoanType result = loanTypeMapper.toDomain(entity);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getLoanTypeId());
        assertEquals("PERSONAL", result.getName());
        assertNull(result.getInterestRate());
        assertNull(result.getMinimumAmount());
        assertNull(result.getMaxAmount());
        assertNull(result.getAutomaticValidation());
    }

    @Test
    void toDomain_ShouldHandleEmptyStrings_WhenFieldsAreEmpty() {
        // Arrange
        LoanTypeEntity entity = LoanTypeEntity.builder()
                .loanTypeId(1L)
                .name("")
                .interestRate(BigDecimal.ZERO)
                .minimumAmount(BigDecimal.ZERO)
                .maxAmount(BigDecimal.ZERO)
                .automaticValidation(false)
                .build();

        // Act
        LoanType result = loanTypeMapper.toDomain(entity);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getLoanTypeId());
        assertEquals("", result.getName());
        assertEquals(BigDecimal.ZERO, result.getInterestRate());
        assertEquals(BigDecimal.ZERO, result.getMinimumAmount());
        assertEquals(BigDecimal.ZERO, result.getMaxAmount());
        assertEquals(false, result.getAutomaticValidation());
    }
}
