package co.com.crediya.solicitudes.r2dbc.mapper;

import co.com.crediya.solicitudes.model.state.State;
import co.com.crediya.solicitudes.r2dbc.entity.StateEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {StateMapperImpl.class})
class StateMapperTest {

    @Autowired
    private StateMapper stateMapper;

    static Stream<Arguments> stateTestData() {
        return Stream.of(
                Arguments.of(
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
                        StateEntity.builder()
                                .stateId(2L)
                                .name("En revision")
                                .description("Solicitud en proceso de evaluacion")
                                .build(),
                        State.builder()
                                .stateId(2L)
                                .name("En revision")
                                .description("Solicitud en proceso de evaluacion")
                                .build()
                ),
                Arguments.of(
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
    void toDomain_ShouldMapEntityToState_WhenValidEntity(StateEntity entity, State expectedState) {
        // Act
        State result = stateMapper.toDomain(entity);

        // Assert
        assertNotNull(result);
        assertEquals(expectedState.getStateId(), result.getStateId());
        assertEquals(expectedState.getName(), result.getName());
        assertEquals(expectedState.getDescription(), result.getDescription());
    }

    @ParameterizedTest
    @NullSource
    void toDomain_ShouldReturnNull_WhenEntityIsNull(StateEntity entity) {
        // Act
        State result = stateMapper.toDomain(entity);

        // Assert
        assertNull(result);
    }

    @Test
    void toDomain_ShouldMapPartialData_WhenSomeFieldsAreNull() {
        // Arrange
        StateEntity entity = StateEntity.builder()
                .stateId(1L)
                .name("Pendiente de revision")
                .build();

        // Act
        State result = stateMapper.toDomain(entity);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getStateId());
        assertEquals("Pendiente de revision", result.getName());
        assertNull(result.getDescription());
    }

    @Test
    void toDomain_ShouldHandleEmptyStrings_WhenFieldsAreEmpty() {
        // Arrange
        StateEntity entity = StateEntity.builder()
                .stateId(1L)
                .name("")
                .description("")
                .build();

        // Act
        State result = stateMapper.toDomain(entity);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getStateId());
        assertEquals("", result.getName());
        assertEquals("", result.getDescription());
    }
}
