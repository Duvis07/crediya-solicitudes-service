package co.com.crediya.solicitudes.r2dbc.adapter;

import co.com.crediya.solicitudes.model.loantype.LoanType;
import co.com.crediya.solicitudes.r2dbc.entity.LoanTypeEntity;
import co.com.crediya.solicitudes.r2dbc.mapper.LoanTypeMapper;
import co.com.crediya.solicitudes.r2dbc.repository.LoanTypeEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanTypeRepositoryAdapterTest {

    @Mock
    private LoanTypeEntityRepository loanTypeEntityRepository;

    @Mock
    private LoanTypeMapper loanTypeMapper;

    private LoanTypeRepositoryAdapter loanTypeRepositoryAdapter;

    @BeforeEach
    void setUp() {
        loanTypeRepositoryAdapter = new LoanTypeRepositoryAdapter(
                loanTypeEntityRepository,
                loanTypeMapper
        );
    }

    @Test
    void findByNameShouldReturnLoanTypeWhenLoanTypeExists() {
        // Arrange
        String loanTypeName = "Prestamo Personal";

        LoanTypeEntity loanTypeEntity = LoanTypeEntity.builder()
                .loanTypeId(1L)
                .name("Prestamo Personal")
                .interestRate(new BigDecimal("0.1250"))
                .minimumAmount(new BigDecimal("100000.00"))
                .maxAmount(new BigDecimal("50000000.00"))
                .automaticValidation(true)
                .build();

        LoanType loanType = LoanType.builder()
                .loanTypeId(1L)
                .name("Prestamo Personal")
                .interestRate(new BigDecimal("0.1250"))
                .minimumAmount(new BigDecimal("100000.00"))
                .maxAmount(new BigDecimal("50000000.00"))
                .automaticValidation(true)
                .build();

        when(loanTypeEntityRepository.findByName(loanTypeName))
                .thenReturn(Mono.just(loanTypeEntity));
        when(loanTypeMapper.toDomain(loanTypeEntity))
                .thenReturn(loanType);

        // Act
        Mono<LoanType> result = loanTypeRepositoryAdapter.findByName(loanTypeName);

        // Assert
        StepVerifier.create(result)
                .assertNext(foundLoanType -> {
                    assertNotNull(foundLoanType);
                    assertEquals(1L, foundLoanType.getLoanTypeId());
                    assertEquals("Prestamo Personal", foundLoanType.getName());
                    assertEquals(new BigDecimal("0.1250"), foundLoanType.getInterestRate());
                    assertEquals(new BigDecimal("100000.00"), foundLoanType.getMinimumAmount());
                    assertEquals(new BigDecimal("50000000.00"), foundLoanType.getMaxAmount());
                    assertTrue(foundLoanType.getAutomaticValidation());
                })
                .verifyComplete();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Nonexistent Loan Type"})
    void findByNameShouldReturnEmptyWhenLoanTypeNotFoundOrInvalidName(String loanTypeName) {
        // Arrange
        when(loanTypeEntityRepository.findByName(loanTypeName))
                .thenReturn(Mono.empty());

        // Act
        Mono<LoanType> result = loanTypeRepositoryAdapter.findByName(loanTypeName);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void findByNameShouldHandleErrorWhenRepositoryFails() {
        // Arrange
        String loanTypeName = "Prestamo Personal";

        when(loanTypeEntityRepository.findByName(loanTypeName))
                .thenReturn(Mono.error(new RuntimeException("Database connection error")));

        // Act
        Mono<LoanType> result = loanTypeRepositoryAdapter.findByName(loanTypeName);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void findByNameShouldCallMapperCorrectlyWhenEntityFound() {
        // Arrange
        String loanTypeName = "Prestamo Hipotecario";

        LoanTypeEntity loanTypeEntity = LoanTypeEntity.builder()
                .loanTypeId(2L)
                .name("Prestamo Hipotecario")
                .interestRate(new BigDecimal("0.0890"))
                .minimumAmount(new BigDecimal("10000000.00"))
                .maxAmount(new BigDecimal("500000000.00"))
                .automaticValidation(false)
                .build();

        LoanType loanType = LoanType.builder()
                .loanTypeId(2L)
                .name("Prestamo Hipotecario")
                .interestRate(new BigDecimal("0.0890"))
                .minimumAmount(new BigDecimal("10000000.00"))
                .maxAmount(new BigDecimal("500000000.00"))
                .automaticValidation(false)
                .build();

        when(loanTypeEntityRepository.findByName(loanTypeName))
                .thenReturn(Mono.just(loanTypeEntity));
        when(loanTypeMapper.toDomain(loanTypeEntity))
                .thenReturn(loanType);

        // Act
        Mono<LoanType> result = loanTypeRepositoryAdapter.findByName(loanTypeName);

        // Assert
        StepVerifier.create(result)
                .assertNext(foundLoanType -> {
                    assertEquals(2L, foundLoanType.getLoanTypeId());
                    assertEquals("Prestamo Hipotecario", foundLoanType.getName());
                    assertFalse(foundLoanType.getAutomaticValidation());
                })
                .verifyComplete();
    }

    @Test
    void findByIdShouldReturnLoanTypeWhenLoanTypeExists() {
        // Arrange
        Long loanTypeId = 1L;

        LoanTypeEntity loanTypeEntity = LoanTypeEntity.builder()
                .loanTypeId(1L)
                .name("Prestamo Personal")
                .interestRate(new BigDecimal("0.1250"))
                .minimumAmount(new BigDecimal("100000.00"))
                .maxAmount(new BigDecimal("50000000.00"))
                .automaticValidation(true)
                .build();

        LoanType loanType = LoanType.builder()
                .loanTypeId(1L)
                .name("Prestamo Personal")
                .interestRate(new BigDecimal("0.1250"))
                .minimumAmount(new BigDecimal("100000.00"))
                .maxAmount(new BigDecimal("50000000.00"))
                .automaticValidation(true)
                .build();

        when(loanTypeEntityRepository.findById(loanTypeId))
                .thenReturn(Mono.just(loanTypeEntity));
        when(loanTypeMapper.toDomain(loanTypeEntity))
                .thenReturn(loanType);

        // Act
        Mono<LoanType> result = loanTypeRepositoryAdapter.findById(loanTypeId);

        // Assert
        StepVerifier.create(result)
                .assertNext(foundLoanType -> {
                    assertNotNull(foundLoanType);
                    assertEquals(1L, foundLoanType.getLoanTypeId());
                    assertEquals("Prestamo Personal", foundLoanType.getName());
                    assertEquals(new BigDecimal("0.1250"), foundLoanType.getInterestRate());
                    assertEquals(new BigDecimal("100000.00"), foundLoanType.getMinimumAmount());
                    assertEquals(new BigDecimal("50000000.00"), foundLoanType.getMaxAmount());
                    assertTrue(foundLoanType.getAutomaticValidation());
                })
                .verifyComplete();
    }

    @Test
    void findByIdShouldReturnEmptyWhenLoanTypeNotFound() {
        // Arrange
        Long loanTypeId = 999L;

        when(loanTypeEntityRepository.findById(loanTypeId))
                .thenReturn(Mono.empty());

        // Act
        Mono<LoanType> result = loanTypeRepositoryAdapter.findById(loanTypeId);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void findByIdShouldHandleErrorWhenRepositoryFails() {
        // Arrange
        Long loanTypeId = 1L;

        when(loanTypeEntityRepository.findById(loanTypeId))
                .thenReturn(Mono.error(new RuntimeException("Database connection error")));

        // Act
        Mono<LoanType> result = loanTypeRepositoryAdapter.findById(loanTypeId);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @ParameterizedTest
    @ValueSource(longs = {2L, 3L})
    void findByIdShouldReturnCorrectLoanTypeForDifferentIds(Long loanTypeId) {
        // Arrange
        LoanTypeEntity loanTypeEntity = LoanTypeEntity.builder()
                .loanTypeId(loanTypeId)
                .name("Prestamo Vehicular")
                .interestRate(new BigDecimal("0.1150"))
                .minimumAmount(new BigDecimal("5000000.00"))
                .maxAmount(new BigDecimal("100000000.00"))
                .automaticValidation(true)
                .build();

        LoanType loanType = LoanType.builder()
                .loanTypeId(loanTypeId)
                .name("Prestamo Vehicular")
                .interestRate(new BigDecimal("0.1150"))
                .minimumAmount(new BigDecimal("5000000.00"))
                .maxAmount(new BigDecimal("100000000.00"))
                .automaticValidation(true)
                .build();

        when(loanTypeEntityRepository.findById(loanTypeId))
                .thenReturn(Mono.just(loanTypeEntity));
        when(loanTypeMapper.toDomain(loanTypeEntity))
                .thenReturn(loanType);

        // Act
        Mono<LoanType> result = loanTypeRepositoryAdapter.findById(loanTypeId);

        // Assert
        StepVerifier.create(result)
                .assertNext(foundLoanType -> {
                    assertEquals(loanTypeId, foundLoanType.getLoanTypeId());
                    assertEquals("Prestamo Vehicular", foundLoanType.getName());
                    assertEquals(new BigDecimal("0.1150"), foundLoanType.getInterestRate());
                })
                .verifyComplete();
    }
}