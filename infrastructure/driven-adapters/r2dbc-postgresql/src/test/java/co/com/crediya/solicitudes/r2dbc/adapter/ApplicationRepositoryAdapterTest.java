package co.com.crediya.solicitudes.r2dbc.adapter;

import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.r2dbc.entity.ApplicationEntity;
import co.com.crediya.solicitudes.r2dbc.mapper.ApplicationMapper;
import co.com.crediya.solicitudes.r2dbc.repository.ApplicationEntityRepository;
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
class ApplicationRepositoryAdapterTest {

    @Mock
    private ApplicationEntityRepository applicationEntityRepository;

    @Mock
    private ApplicationMapper applicationMapper;

    private ApplicationRepositoryAdapter applicationRepositoryAdapter;

    @BeforeEach
    void setUp() {
        applicationRepositoryAdapter = new ApplicationRepositoryAdapter(
                applicationEntityRepository,
                applicationMapper
        );
    }

    @Test
    void save_ShouldReturnApplication_WhenValidApplication() {
        // Arrange
        Application inputApplication = Application.builder()
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(12)
                .loanTypeId(1L)
                .stateId(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ApplicationEntity applicationEntity = ApplicationEntity.builder()
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(12)
                .loanTypeId(1L)
                .stateId(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ApplicationEntity savedEntity = ApplicationEntity.builder()
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

        when(applicationMapper.toEntity(inputApplication)).thenReturn(applicationEntity);
        when(applicationEntityRepository.save(applicationEntity)).thenReturn(Mono.just(savedEntity));
        when(applicationMapper.toDomain(savedEntity)).thenReturn(savedApplication);

        // Act
        Mono<Application> result = applicationRepositoryAdapter.save(inputApplication);

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
    void save_ShouldHandleError_WhenRepositoryFails() {
        // Arrange
        Application inputApplication = Application.builder()
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(12)
                .build();

        ApplicationEntity applicationEntity = ApplicationEntity.builder()
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(12)
                .build();

        when(applicationMapper.toEntity(inputApplication)).thenReturn(applicationEntity);
        when(applicationEntityRepository.save(applicationEntity))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        // Act
        Mono<Application> result = applicationRepositoryAdapter.save(inputApplication);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void findAll_ShouldReturnAllApplications_WhenApplicationsExist() {
        // Arrange
        ApplicationEntity entity1 = ApplicationEntity.builder()
                .applicationId(1L)
                .documentId("12345678")
                .email("test1@example.com")
                .amount(new BigDecimal("500000"))
                .term(12)
                .loanTypeId(1L)
                .stateId(1L)
                .build();

        ApplicationEntity entity2 = ApplicationEntity.builder()
                .applicationId(2L)
                .documentId("87654321")
                .email("test2@example.com")
                .amount(new BigDecimal("1000000"))
                .term(24)
                .loanTypeId(2L)
                .stateId(1L)
                .build();

        Application app1 = Application.builder()
                .applicationId(1L)
                .documentId("12345678")
                .email("test1@example.com")
                .amount(new BigDecimal("500000"))
                .term(12)
                .loanTypeId(1L)
                .stateId(1L)
                .build();

        Application app2 = Application.builder()
                .applicationId(2L)
                .documentId("87654321")
                .email("test2@example.com")
                .amount(new BigDecimal("1000000"))
                .term(24)
                .loanTypeId(2L)
                .stateId(1L)
                .build();

        when(applicationEntityRepository.findAll()).thenReturn(Flux.just(entity1, entity2));
        when(applicationMapper.toDomain(entity1)).thenReturn(app1);
        when(applicationMapper.toDomain(entity2)).thenReturn(app2);

        // Act
        Flux<Application> result = applicationRepositoryAdapter.findAll();

        // Assert
        StepVerifier.create(result)
                .assertNext(application -> {
                    assertEquals(1L, application.getApplicationId());
                    assertEquals("12345678", application.getDocumentId());
                    assertEquals("test1@example.com", application.getEmail());
                })
                .assertNext(application -> {
                    assertEquals(2L, application.getApplicationId());
                    assertEquals("87654321", application.getDocumentId());
                    assertEquals("test2@example.com", application.getEmail());
                })
                .verifyComplete();
    }

    @Test
    void findAll_ShouldReturnEmpty_WhenNoApplicationsExist() {
        // Arrange
        when(applicationEntityRepository.findAll()).thenReturn(Flux.empty());

        // Act
        Flux<Application> result = applicationRepositoryAdapter.findAll();

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void findAll_ShouldHandleError_WhenRepositoryFails() {
        // Arrange
        when(applicationEntityRepository.findAll())
                .thenReturn(Flux.error(new RuntimeException("Database connection error")));

        // Act
        Flux<Application> result = applicationRepositoryAdapter.findAll();

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void save_ShouldCallMapperMethods_InCorrectOrder() {
        // Arrange
        Application inputApplication = Application.builder()
                .documentId("12345678")
                .build();

        ApplicationEntity entity = ApplicationEntity.builder()
                .documentId("12345678")
                .build();

        ApplicationEntity savedEntity = ApplicationEntity.builder()
                .applicationId(1L)
                .documentId("12345678")
                .build();

        Application savedApplication = Application.builder()
                .applicationId(1L)
                .documentId("12345678")
                .build();

        when(applicationMapper.toEntity(any(Application.class))).thenReturn(entity);
        when(applicationEntityRepository.save(any(ApplicationEntity.class))).thenReturn(Mono.just(savedEntity));
        when(applicationMapper.toDomain(any(ApplicationEntity.class))).thenReturn(savedApplication);

        // Act
        Mono<Application> result = applicationRepositoryAdapter.save(inputApplication);

        // Assert
        StepVerifier.create(result)
                .assertNext(application -> {
                    assertNotNull(application);
                    assertEquals(1L, application.getApplicationId());
                })
                .verifyComplete();
    }
}
