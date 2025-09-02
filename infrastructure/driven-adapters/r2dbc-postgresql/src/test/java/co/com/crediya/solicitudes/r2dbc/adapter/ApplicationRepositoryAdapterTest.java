package co.com.crediya.solicitudes.r2dbc.adapter;

import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.common.PageRequest;
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
import java.util.Arrays;
import java.util.List;

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
    void saveShouldReturnApplicationWhenValidApplication() {
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
    void saveShouldHandleErrorWhenRepositoryFails() {
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
    void saveShouldCallMapperMethodsInCorrectOrder() {
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

    @Test
    void findByStateInWithPaginationShouldReturnApplicationsWhenValidStates() {
        // Arrange
        List<Long> stateIds = Arrays.asList(1L, 2L, 3L);
        PageRequest pageRequest = PageRequest.of(0, 10);
        
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
                .stateId(2L)
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
                .stateId(2L)
                .build();

        when(applicationEntityRepository.findByStateIdInWithPagination(stateIds, 10, 0L))
                .thenReturn(Flux.just(entity1, entity2));
        when(applicationMapper.toDomain(entity1)).thenReturn(app1);
        when(applicationMapper.toDomain(entity2)).thenReturn(app2);

        // Act
        Flux<Application> result = applicationRepositoryAdapter.findByStateInWithPagination(stateIds, pageRequest);

        // Assert
        StepVerifier.create(result)
                .assertNext(application -> {
                    assertEquals(1L, application.getApplicationId());
                    assertEquals("12345678", application.getDocumentId());
                    assertEquals("test1@example.com", application.getEmail());
                    assertEquals(1L, application.getStateId());
                })
                .assertNext(application -> {
                    assertEquals(2L, application.getApplicationId());
                    assertEquals("87654321", application.getDocumentId());
                    assertEquals("test2@example.com", application.getEmail());
                    assertEquals(2L, application.getStateId());
                })
                .verifyComplete();
    }

    @Test
    void findByStateInWithPaginationShouldReturnEmptyWhenNoApplicationsFound() {
        // Arrange
        List<Long> stateIds = Arrays.asList(1L, 2L, 3L);
        PageRequest pageRequest = PageRequest.of(0, 10);

        when(applicationEntityRepository.findByStateIdInWithPagination(stateIds, 10, 0L))
                .thenReturn(Flux.empty());

        // Act
        Flux<Application> result = applicationRepositoryAdapter.findByStateInWithPagination(stateIds, pageRequest);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void countByStateInShouldReturnCountWhenValidStates() {
        // Arrange
        List<Long> stateIds = Arrays.asList(1L, 2L, 3L);
        Long expectedCount = 5L;

        when(applicationEntityRepository.countByStateIdIn(stateIds))
                .thenReturn(Mono.just(expectedCount));

        // Act
        Mono<Long> result = applicationRepositoryAdapter.countByStateIn(stateIds);

        // Assert
        StepVerifier.create(result)
                .assertNext(count -> assertEquals(expectedCount, count))
                .verifyComplete();
    }

    @Test
    void countByStateInShouldReturnZeroWhenNoApplicationsFound() {
        // Arrange
        List<Long> stateIds = Arrays.asList(1L, 2L, 3L);

        when(applicationEntityRepository.countByStateIdIn(stateIds))
                .thenReturn(Mono.just(0L));

        // Act
        Mono<Long> result = applicationRepositoryAdapter.countByStateIn(stateIds);

        // Assert
        StepVerifier.create(result)
                .assertNext(count -> assertEquals(0L, count))
                .verifyComplete();
    }

    @Test
    void findByDocumentIdAndStateIdShouldReturnApplicationsWhenFound() {
        // Arrange
        String documentId = "12345678";
        Long stateId = 1L;

        ApplicationEntity entity = ApplicationEntity.builder()
                .applicationId(1L)
                .documentId(documentId)
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(12)
                .loanTypeId(1L)
                .stateId(stateId)
                .build();

        Application application = Application.builder()
                .applicationId(1L)
                .documentId(documentId)
                .email("test@example.com")
                .amount(new BigDecimal("500000"))
                .term(12)
                .loanTypeId(1L)
                .stateId(stateId)
                .build();

        when(applicationEntityRepository.findByDocumentIdAndStateId(documentId, stateId))
                .thenReturn(Flux.just(entity));
        when(applicationMapper.toDomain(entity)).thenReturn(application);

        // Act
        Flux<Application> result = applicationRepositoryAdapter.findByDocumentIdAndStateId(documentId, stateId);

        // Assert
        StepVerifier.create(result)
                .assertNext(app -> {
                    assertEquals(1L, app.getApplicationId());
                    assertEquals(documentId, app.getDocumentId());
                    assertEquals("test@example.com", app.getEmail());
                    assertEquals(stateId, app.getStateId());
                })
                .verifyComplete();
    }

    @Test
    void findByDocumentIdAndStateIdShouldReturnEmptyWhenNotFound() {
        // Arrange
        String documentId = "12345678";
        Long stateId = 1L;

        when(applicationEntityRepository.findByDocumentIdAndStateId(documentId, stateId))
                .thenReturn(Flux.empty());

        // Act
        Flux<Application> result = applicationRepositoryAdapter.findByDocumentIdAndStateId(documentId, stateId);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }
}
