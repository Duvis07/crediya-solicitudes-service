package co.com.crediya.solicitudes.r2dbc.mapper;

import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.r2dbc.entity.ApplicationEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ApplicationMapperImpl.class})
class ApplicationMapperTest {

    @Autowired
    private ApplicationMapper applicationMapper;

    @Test
    void toEntity_ShouldMapApplicationToEntity_WhenValidApplication() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Application application = Application.builder()
                .applicationId(1L)
                .documentId("12345678")
                .amount(new BigDecimal("500000"))
                .term(24)
                .email("test@example.com")
                .stateId(1L)
                .loanTypeId(2L)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Act
        ApplicationEntity result = applicationMapper.toEntity(application);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getApplicationId());
        assertEquals("12345678", result.getDocumentId());
        assertEquals(new BigDecimal("500000"), result.getAmount());
        assertEquals(24, result.getTerm());
        assertEquals("test@example.com", result.getEmail());
        assertEquals(1L, result.getStateId());
        assertEquals(2L, result.getLoanTypeId());
        assertEquals(now, result.getCreatedAt());
        assertEquals(now, result.getUpdatedAt());
    }

    @Test
    void toDomain_ShouldMapEntityToApplication_WhenValidEntity() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        ApplicationEntity entity = ApplicationEntity.builder()
                .applicationId(1L)
                .documentId("12345678")
                .amount(new BigDecimal("500000"))
                .term(24)
                .email("test@example.com")
                .stateId(1L)
                .loanTypeId(2L)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Act
        Application result = applicationMapper.toDomain(entity);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getApplicationId());
        assertEquals("12345678", result.getDocumentId());
        assertEquals(new BigDecimal("500000"), result.getAmount());
        assertEquals(24, result.getTerm());
        assertEquals("test@example.com", result.getEmail());
        assertEquals(1L, result.getStateId());
        assertEquals(2L, result.getLoanTypeId());
        assertEquals(now, result.getCreatedAt());
        assertEquals(now, result.getUpdatedAt());
    }

    @ParameterizedTest
    @NullSource
    void toEntity_ShouldReturnNull_WhenApplicationIsNull(Application application) {
        // Act
        ApplicationEntity result = applicationMapper.toEntity(application);

        // Assert
        assertNull(result);
    }

    @ParameterizedTest
    @NullSource
    void toDomain_ShouldReturnNull_WhenEntityIsNull(ApplicationEntity entity) {
        // Act
        Application result = applicationMapper.toDomain(entity);

        // Assert
        assertNull(result);
    }

    @Test
    void toEntity_ShouldMapPartialData_WhenSomeFieldsAreNull() {
        // Arrange
        Application application = Application.builder()
                .documentId("12345678")
                .amount(new BigDecimal("500000"))
                .email("test@example.com")
                .build();

        // Act
        ApplicationEntity result = applicationMapper.toEntity(application);

        // Assert
        assertNotNull(result);
        assertNull(result.getApplicationId());
        assertEquals("12345678", result.getDocumentId());
        assertEquals(new BigDecimal("500000"), result.getAmount());
        assertNull(result.getTerm());
        assertEquals("test@example.com", result.getEmail());
        assertNull(result.getStateId());
        assertNull(result.getLoanTypeId());
        assertNull(result.getCreatedAt());
        assertNull(result.getUpdatedAt());
    }

    @Test
    void toDomain_ShouldMapPartialData_WhenSomeFieldsAreNull() {
        // Arrange
        ApplicationEntity entity = ApplicationEntity.builder()
                .documentId("12345678")
                .amount(new BigDecimal("500000"))
                .email("test@example.com")
                .build();

        // Act
        Application result = applicationMapper.toDomain(entity);

        // Assert
        assertNotNull(result);
        assertNull(result.getApplicationId());
        assertEquals("12345678", result.getDocumentId());
        assertEquals(new BigDecimal("500000"), result.getAmount());
        assertNull(result.getTerm());
        assertEquals("test@example.com", result.getEmail());
        assertNull(result.getStateId());
        assertNull(result.getLoanTypeId());
        assertNull(result.getCreatedAt());
        assertNull(result.getUpdatedAt());
    }
}
