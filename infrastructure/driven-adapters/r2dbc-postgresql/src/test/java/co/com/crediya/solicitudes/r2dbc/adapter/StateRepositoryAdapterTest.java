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
    void findByName_ShouldReturnState_WhenStateExists(String stateName, StateEntity entity, State expectedState) {
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
    void findByName_ShouldReturnEmpty_WhenStateNotFoundOrInvalidName(String stateName) {
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
    void findByName_ShouldHandleError_WhenRepositoryFails() {
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
    void findByName_ShouldReturnDifferentStates_WhenDifferentNamesProvided() {
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
}