package co.com.crediya.solicitudes.r2dbc.adapter;

import co.com.crediya.solicitudes.model.state.State;
import co.com.crediya.solicitudes.r2dbc.entity.StateEntity;
import co.com.crediya.solicitudes.r2dbc.mapper.StateMapper;
import co.com.crediya.solicitudes.r2dbc.repository.StateEntityRepository;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.stream.Stream;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StateRepositoryAdapterTest {

    @Mock
    private StateEntityRepository stateEntityRepository;

    @Mock
    private StateMapper stateMapper;

    private StateRepositoryAdapter stateRepositoryAdapter;

    @BeforeEach
    void setUp() {
        stateRepositoryAdapter = new StateRepositoryAdapter(
                stateEntityRepository,
                stateMapper
        );
    }

    static Stream<Arguments> stateTestData() {
        return Stream.of(
                Arguments.of(
                        "Pendiente de revision",
                        StateEntity.builder()
                                .stateId(1L)
                                .name("Pendiente de revision")
                                .description("Solicitud recibida, pendiente de evaluacion inicial")
                                .build(),
                        State.builder()
                                .stateId(1L)
                                .name("Pendiente de revision")
                                .description("Solicitud recibida, pendiente de evaluacion inicial")
                                .build()
                ),
                Arguments.of(
                        "Aprobada",
                        StateEntity.builder()
                                .stateId(3L)
                                .name("Aprobada")
                                .description("Solicitud aprobada, pendiente de desembolso")
                                .build(),
                        State.builder()
                                .stateId(3L)
                                .name("Aprobada")
                                .description("Solicitud aprobada, pendiente de desembolso")
                                .build()
                ),
                Arguments.of(
                        "Rechazada",
                        StateEntity.builder()
                                .stateId(4L)
                                .name("Rechazada")
                                .description("Solicitud rechazada por no cumplir criterios")
                                .build(),
                        State.builder()
                                .stateId(4L)
                                .name("Rechazada")
                                .description("Solicitud rechazada por no cumplir criterios")
                                .build()
                )
        );
    }

    @ParameterizedTest
    @MethodSource("stateTestData")
    void findByNameShouldReturnStateWhenStateExists(String stateName, StateEntity entity, State expectedState) {
        // Arrange
        when(stateEntityRepository.findByName(stateName))
                .thenReturn(Mono.just(entity));
        when(stateMapper.toDomain(entity))
                .thenReturn(expectedState);

        // Act
        Mono<State> result = stateRepositoryAdapter.findByName(stateName);

        // Assert
        StepVerifier.create(result)
                .assertNext(foundState -> {
                    assertNotNull(foundState);
                    assertEquals(expectedState.getStateId(), foundState.getStateId());
                    assertEquals(expectedState.getName(), foundState.getName());
                    assertEquals(expectedState.getDescription(), foundState.getDescription());
                })
                .verifyComplete();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Nonexistent State"})
    void findByNameShouldReturnEmptyWhenStateNotFoundOrInvalidName(String stateName) {
        // Arrange
        when(stateEntityRepository.findByName(stateName))
                .thenReturn(Mono.empty());

        // Act
        Mono<State> result = stateRepositoryAdapter.findByName(stateName);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void findByNameShouldHandleErrorWhenRepositoryFails() {
        // Arrange
        String stateName = "Pendiente de revision";

        when(stateEntityRepository.findByName(stateName))
                .thenReturn(Mono.error(new RuntimeException("Database connection error")));

        // Act
        Mono<State> result = stateRepositoryAdapter.findByName(stateName);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void findByNameShouldReturnDifferentStatesWhenDifferentNamesProvided() {
        // Arrange
        String rejectedStateName = "Rechazada";

        StateEntity rejectedEntity = StateEntity.builder()
                .stateId(4L)
                .name("Rechazada")
                .description("Solicitud rechazada por no cumplir criterios")
                .build();

        State rejectedState = State.builder()
                .stateId(4L)
                .name("Rechazada")
                .description("Solicitud rechazada por no cumplir criterios")
                .build();

        when(stateEntityRepository.findByName(rejectedStateName))
                .thenReturn(Mono.just(rejectedEntity));
        when(stateMapper.toDomain(rejectedEntity))
                .thenReturn(rejectedState);

        // Act
        Mono<State> result = stateRepositoryAdapter.findByName(rejectedStateName);

        // Assert
        StepVerifier.create(result)
                .assertNext(foundState -> {
                    assertEquals(4L, foundState.getStateId());
                    assertEquals("Rechazada", foundState.getName());
                    assertEquals("Solicitud rechazada por no cumplir criterios", foundState.getDescription());
                })
                .verifyComplete();
    }

    @Test
    void findByIdShouldReturnStateWhenStateExists() {
        // Arrange
        Long stateId = 1L;

        StateEntity stateEntity = StateEntity.builder()
                .stateId(1L)
                .name("Pendiente de revision")
                .description("Solicitud recibida, pendiente de evaluacion inicial")
                .build();

        State state = State.builder()
                .stateId(1L)
                .name("Pendiente de revision")
                .description("Solicitud recibida, pendiente de evaluacion inicial")
                .build();

        when(stateEntityRepository.findById(stateId))
                .thenReturn(Mono.just(stateEntity));
        when(stateMapper.toDomain(stateEntity))
                .thenReturn(state);

        // Act
        Mono<State> result = stateRepositoryAdapter.findById(stateId);

        // Assert
        StepVerifier.create(result)
                .assertNext(foundState -> {
                    assertNotNull(foundState);
                    assertEquals(1L, foundState.getStateId());
                    assertEquals("Pendiente de revision", foundState.getName());
                    assertEquals("Solicitud recibida, pendiente de evaluacion inicial", foundState.getDescription());
                })
                .verifyComplete();
    }

    @Test
    void findByIdShouldReturnEmptyWhenStateNotFound() {
        // Arrange
        Long stateId = 999L;

        when(stateEntityRepository.findById(stateId))
                .thenReturn(Mono.empty());

        // Act
        Mono<State> result = stateRepositoryAdapter.findById(stateId);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void findByIdShouldHandleErrorWhenRepositoryFails() {
        // Arrange
        Long stateId = 1L;

        when(stateEntityRepository.findById(stateId))
                .thenReturn(Mono.error(new RuntimeException("Database connection error")));

        // Act
        Mono<State> result = stateRepositoryAdapter.findById(stateId);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @ParameterizedTest
    @ValueSource(longs = {2L, 3L, 4L})
    void findByIdShouldReturnCorrectStateForDifferentIds(Long stateId) {
        // Arrange
        StateEntity stateEntity = StateEntity.builder()
                .stateId(stateId)
                .name("Test State")
                .description("Test Description")
                .build();

        State state = State.builder()
                .stateId(stateId)
                .name("Test State")
                .description("Test Description")
                .build();

        when(stateEntityRepository.findById(stateId))
                .thenReturn(Mono.just(stateEntity));
        when(stateMapper.toDomain(stateEntity))
                .thenReturn(state);

        // Act
        Mono<State> result = stateRepositoryAdapter.findById(stateId);

        // Assert
        StepVerifier.create(result)
                .assertNext(foundState -> {
                    assertEquals(stateId, foundState.getStateId());
                    assertEquals("Test State", foundState.getName());
                    assertEquals("Test Description", foundState.getDescription());
                })
                .verifyComplete();
    }

    static Stream<Arguments> stateIdTestData() {
        return Stream.of(
                Arguments.of(
                        1L,
                        StateEntity.builder()
                                .stateId(1L)
                                .name("Pendiente de revision")
                                .description("Solicitud recibida, pendiente de evaluacion inicial")
                                .build(),
                        State.builder()
                                .stateId(1L)
                                .name("Pendiente de revision")
                                .description("Solicitud recibida, pendiente de evaluacion inicial")
                                .build()
                ),
                Arguments.of(
                        3L,
                        StateEntity.builder()
                                .stateId(3L)
                                .name("Aprobada")
                                .description("Solicitud aprobada, pendiente de desembolso")
                                .build(),
                        State.builder()
                                .stateId(3L)
                                .name("Aprobada")
                                .description("Solicitud aprobada, pendiente de desembolso")
                                .build()
                ),
                Arguments.of(
                        4L,
                        StateEntity.builder()
                                .stateId(4L)
                                .name("Rechazada")
                                .description("Solicitud rechazada por no cumplir criterios")
                                .build(),
                        State.builder()
                                .stateId(4L)
                                .name("Rechazada")
                                .description("Solicitud rechazada por no cumplir criterios")
                                .build()
                )
        );
    }

    @ParameterizedTest
    @MethodSource("stateIdTestData")
    void findByIdShouldReturnCorrectStateWhenValidId(Long stateId, StateEntity entity, State expectedState) {
        // Arrange
        when(stateEntityRepository.findById(stateId))
                .thenReturn(Mono.just(entity));
        when(stateMapper.toDomain(entity))
                .thenReturn(expectedState);

        // Act
        Mono<State> result = stateRepositoryAdapter.findById(stateId);

        // Assert
        StepVerifier.create(result)
                .assertNext(foundState -> {
                    assertEquals(expectedState.getStateId(), foundState.getStateId());
                    assertEquals(expectedState.getName(), foundState.getName());
                    assertEquals(expectedState.getDescription(), foundState.getDescription());
                })
                .verifyComplete();
    }
}