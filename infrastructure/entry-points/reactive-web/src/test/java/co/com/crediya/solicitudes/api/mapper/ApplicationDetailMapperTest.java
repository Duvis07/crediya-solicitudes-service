package co.com.crediya.solicitudes.api.mapper;

import co.com.crediya.solicitudes.api.dto.ApplicationDetailResponse;
import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.client.UserType;
import co.com.crediya.solicitudes.webclient.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationDetailMapperTest {

    private ApplicationDetailMapper applicationDetailMapper;

    @BeforeEach
    void setUp() {
        applicationDetailMapper = new ApplicationDetailMapper();
    }

    @Test
    void toDetailResponseShouldMapAllFieldsCorrectlyWhenValidInputs() {
        // Arrange
        Application application = Application.builder()
                .applicationId(1L)
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000.00"))
                .term(12)
                .loanTypeId(1L)
                .stateId(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UserResponse user = UserResponse.builder()
                .id(1L)
                .firstName("Juan")
                .lastName("Pérez")
                .email("test@example.com")
                .birthDate(LocalDate.of(1990, 5, 15))
                .phone("3001234567")
                .address("Calle 123 #45-67")
                .baseSalary(3000000.0)
                .userType(UserType.APPLICANT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        String applicationState = "Pendiente de revision";
        String loanTypeName = "Prestamo Personal";
        BigDecimal interestRate = new BigDecimal("0.1250");
        BigDecimal totalMonthlyDebt = new BigDecimal("450000.00");

        // Act
        ApplicationDetailResponse result = applicationDetailMapper.toDetailResponse(
                application, user, applicationState, loanTypeName, interestRate, totalMonthlyDebt);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("500000.00"), result.getAmount());
        assertEquals(12, result.getTerm());
        assertEquals("Pendiente de revision", result.getApplicationState());
        assertEquals("12345678", result.getDocumentId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Juan Pérez", result.getFullName());
        assertEquals(new BigDecimal("3000000.0"), result.getBaseSalary());
        assertEquals("Prestamo Personal", result.getLoanTypeName());
        assertEquals(new BigDecimal("0.1250"), result.getInterestRate());
        assertEquals(new BigDecimal("450000.00"), result.getTotalMonthlyDebt());
    }

    @Test
    void toDetailResponseShouldHandleNullUserNamesCorrectly() {
        // Arrange
        Application application = Application.builder()
                .documentId("87654321")
                .email("test2@example.com")
                .amount(new BigDecimal("1000000.00"))
                .term(24)
                .build();

        UserResponse user = UserResponse.builder()
                .id(2L)
                .firstName(null)
                .lastName(null)
                .email("test2@example.com")
                .baseSalary(5000000.0)
                .userType(UserType.APPLICANT)
                .build();

        String applicationState = "Aprobada";
        String loanTypeName = "Prestamo Hipotecario";
        BigDecimal interestRate = new BigDecimal("0.0890");
        BigDecimal totalMonthlyDebt = new BigDecimal("750000.00");

        // Act
        ApplicationDetailResponse result = applicationDetailMapper.toDetailResponse(
                application, user, applicationState, loanTypeName, interestRate, totalMonthlyDebt);

        // Assert
        assertNotNull(result);
        assertEquals("", result.getFullName());
        assertEquals(new BigDecimal("1000000.00"), result.getAmount());
        assertEquals(24, result.getTerm());
        assertEquals("Aprobada", result.getApplicationState());
        assertEquals("87654321", result.getDocumentId());
        assertEquals("test2@example.com", result.getEmail());
        assertEquals(new BigDecimal("5000000.0"), result.getBaseSalary());
        assertEquals("Prestamo Hipotecario", result.getLoanTypeName());
        assertEquals(new BigDecimal("0.0890"), result.getInterestRate());
        assertEquals(new BigDecimal("750000.00"), result.getTotalMonthlyDebt());
    }

    @Test
    void toDetailResponseShouldHandleOnlyFirstNameCorrectly() {
        // Arrange
        Application application = Application.builder()
                .documentId("11111111")
                .email("maria@example.com")
                .amount(new BigDecimal("300000.00"))
                .term(6)
                .build();

        UserResponse user = UserResponse.builder()
                .id(3L)
                .firstName("María")
                .lastName(null)
                .email("maria@example.com")
                .baseSalary(2500000.0)
                .userType(UserType.APPLICANT)
                .build();

        String applicationState = "Rechazada";
        String loanTypeName = "Prestamo Vehicular";
        BigDecimal interestRate = new BigDecimal("0.1150");
        BigDecimal totalMonthlyDebt = new BigDecimal("200000.00");

        // Act
        ApplicationDetailResponse result = applicationDetailMapper.toDetailResponse(
                application, user, applicationState, loanTypeName, interestRate, totalMonthlyDebt);

        // Assert
        assertNotNull(result);
        assertEquals("María", result.getFullName());
        assertEquals(new BigDecimal("300000.00"), result.getAmount());
        assertEquals(6, result.getTerm());
        assertEquals("Rechazada", result.getApplicationState());
        assertEquals("11111111", result.getDocumentId());
        assertEquals("maria@example.com", result.getEmail());
        assertEquals(new BigDecimal("2500000.0"), result.getBaseSalary());
        assertEquals("Prestamo Vehicular", result.getLoanTypeName());
        assertEquals(new BigDecimal("0.1150"), result.getInterestRate());
        assertEquals(new BigDecimal("200000.00"), result.getTotalMonthlyDebt());
    }

    @Test
    void toDetailResponseShouldHandleOnlyLastNameCorrectly() {
        // Arrange
        Application application = Application.builder()
                .documentId("22222222")
                .email("rodriguez@example.com")
                .amount(new BigDecimal("800000.00"))
                .term(18)
                .build();

        UserResponse user = UserResponse.builder()
                .id(4L)
                .firstName(null)
                .lastName("Rodríguez")
                .email("rodriguez@example.com")
                .baseSalary(4000000.0)
                .userType(UserType.APPLICANT)
                .build();

        String applicationState = "Revision manual";
        String loanTypeName = "Prestamo Empresarial";
        BigDecimal interestRate = new BigDecimal("0.0950");
        BigDecimal totalMonthlyDebt = new BigDecimal("600000.00");

        // Act
        ApplicationDetailResponse result = applicationDetailMapper.toDetailResponse(
                application, user, applicationState, loanTypeName, interestRate, totalMonthlyDebt);

        // Assert
        assertNotNull(result);
        assertEquals("Rodríguez", result.getFullName());
        assertEquals(new BigDecimal("800000.00"), result.getAmount());
        assertEquals(18, result.getTerm());
        assertEquals("Revision manual", result.getApplicationState());
        assertEquals("22222222", result.getDocumentId());
        assertEquals("rodriguez@example.com", result.getEmail());
        assertEquals(new BigDecimal("4000000.0"), result.getBaseSalary());
        assertEquals("Prestamo Empresarial", result.getLoanTypeName());
        assertEquals(new BigDecimal("0.0950"), result.getInterestRate());
        assertEquals(new BigDecimal("600000.00"), result.getTotalMonthlyDebt());
    }

    @Test
    void toDetailResponseShouldHandleZeroValuesCorrectly() {
        // Arrange
        Application application = Application.builder()
                .documentId("00000000")
                .email("zero@example.com")
                .amount(BigDecimal.ZERO)
                .term(0)
                .build();

        UserResponse user = UserResponse.builder()
                .id(5L)
                .firstName("Test")
                .lastName("User")
                .email("zero@example.com")
                .baseSalary(0.0)
                .userType(UserType.APPLICANT)
                .build();

        String applicationState = "Pendiente";
        String loanTypeName = "Test Loan";
        BigDecimal interestRate = BigDecimal.ZERO;
        BigDecimal totalMonthlyDebt = BigDecimal.ZERO;

        // Act
        ApplicationDetailResponse result = applicationDetailMapper.toDetailResponse(
                application, user, applicationState, loanTypeName, interestRate, totalMonthlyDebt);

        // Assert
        assertNotNull(result);
        assertEquals("Test User", result.getFullName());
        assertEquals(BigDecimal.ZERO, result.getAmount());
        assertEquals(0, result.getTerm());
        assertEquals("Pendiente", result.getApplicationState());
        assertEquals("00000000", result.getDocumentId());
        assertEquals("zero@example.com", result.getEmail());
        assertEquals(new BigDecimal("0.0"), result.getBaseSalary());
        assertEquals("Test Loan", result.getLoanTypeName());
        assertEquals(BigDecimal.ZERO, result.getInterestRate());
        assertEquals(BigDecimal.ZERO, result.getTotalMonthlyDebt());
    }

    static Stream<Arguments> userNameTestData() {
        return Stream.of(
                Arguments.of("Carlos", "García", "Carlos García"),
                Arguments.of("Ana", "López", "Ana López"),
                Arguments.of("Pedro", null, "Pedro"),
                Arguments.of(null, "Martínez", "Martínez"),
                Arguments.of(null, null, ""),
                Arguments.of("  José  ", "  Silva  ", "José Silva"),
                Arguments.of("", "", ""),
                Arguments.of("María José", "Fernández Ruiz", "María José Fernández Ruiz")
        );
    }

    @ParameterizedTest
    @MethodSource("userNameTestData")
    void toDetailResponseShouldHandleVariousNameCombinations(String firstName, String lastName, String expectedFullName) {
        // Arrange
        Application application = Application.builder()
                .documentId("99999999")
                .email("test@example.com")
                .amount(new BigDecimal("100000.00"))
                .term(12)
                .build();

        UserResponse user = UserResponse.builder()
                .id(99L)
                .firstName(firstName)
                .lastName(lastName)
                .email("test@example.com")
                .baseSalary(1000000.0)
                .userType(UserType.APPLICANT)
                .build();

        String applicationState = "Test State";
        String loanTypeName = "Test Loan";
        BigDecimal interestRate = new BigDecimal("0.1000");
        BigDecimal totalMonthlyDebt = new BigDecimal("100000.00");

        // Act
        ApplicationDetailResponse result = applicationDetailMapper.toDetailResponse(
                application, user, applicationState, loanTypeName, interestRate, totalMonthlyDebt);

        // Assert
        assertNotNull(result);
        assertEquals(expectedFullName, result.getFullName());
    }

    @Test
    void toDetailResponseShouldHandleLargeNumbersCorrectly() {
        // Arrange
        Application application = Application.builder()
                .documentId("77777777")
                .email("large@example.com")
                .amount(new BigDecimal("999999999.99"))
                .term(360)
                .build();

        UserResponse user = UserResponse.builder()
                .id(7L)
                .firstName("Large")
                .lastName("Numbers")
                .email("large@example.com")
                .baseSalary(999999999.99)
                .userType(UserType.APPLICANT)
                .build();

        String applicationState = "Large Test";
        String loanTypeName = "Large Loan";
        BigDecimal interestRate = new BigDecimal("0.9999");
        BigDecimal totalMonthlyDebt = new BigDecimal("999999999.99");

        // Act
        ApplicationDetailResponse result = applicationDetailMapper.toDetailResponse(
                application, user, applicationState, loanTypeName, interestRate, totalMonthlyDebt);

        // Assert
        assertNotNull(result);
        assertEquals("Large Numbers", result.getFullName());
        assertEquals(new BigDecimal("999999999.99"), result.getAmount());
        assertEquals(360, result.getTerm());
        assertEquals("Large Test", result.getApplicationState());
        assertEquals("77777777", result.getDocumentId());
        assertEquals("large@example.com", result.getEmail());
        assertEquals(new BigDecimal("999999999.99"), result.getBaseSalary());
        assertEquals("Large Loan", result.getLoanTypeName());
        assertEquals(new BigDecimal("0.9999"), result.getInterestRate());
        assertEquals(new BigDecimal("999999999.99"), result.getTotalMonthlyDebt());
    }

    @Test
    void toDetailResponseShouldHandleSpecialCharactersInStrings() {
        // Arrange
        Application application = Application.builder()
                .documentId("SPEC-123")
                .email("special@test.co.uk")
                .amount(new BigDecimal("123456.78"))
                .term(15)
                .build();

        UserResponse user = UserResponse.builder()
                .id(8L)
                .firstName("José María")
                .lastName("Ñoño-Pérez")
                .email("special@test.co.uk")
                .baseSalary(2345678.90)
                .userType(UserType.APPLICANT)
                .build();

        String applicationState = "Estado Especial ñáéíóú";
        String loanTypeName = "Préstamo Ñoño";
        BigDecimal interestRate = new BigDecimal("0.1234");
        BigDecimal totalMonthlyDebt = new BigDecimal("567890.12");

        // Act
        ApplicationDetailResponse result = applicationDetailMapper.toDetailResponse(
                application, user, applicationState, loanTypeName, interestRate, totalMonthlyDebt);

        // Assert
        assertNotNull(result);
        assertEquals("José María Ñoño-Pérez", result.getFullName());
        assertEquals(new BigDecimal("123456.78"), result.getAmount());
        assertEquals(15, result.getTerm());
        assertEquals("Estado Especial ñáéíóú", result.getApplicationState());
        assertEquals("SPEC-123", result.getDocumentId());
        assertEquals("special@test.co.uk", result.getEmail());
        assertEquals(new BigDecimal("2345678.9"), result.getBaseSalary());
        assertEquals("Préstamo Ñoño", result.getLoanTypeName());
        assertEquals(new BigDecimal("0.1234"), result.getInterestRate());
        assertEquals(new BigDecimal("567890.12"), result.getTotalMonthlyDebt());
    }
}
