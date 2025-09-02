package co.com.crediya.solicitudes.api.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class UserNameUtilsTest {

    @Test
    void buildFullNameShouldReturnFullNameWhenBothNamesProvided() {
        // Act
        String result = UserNameUtils.buildFullName("Juan", "Pérez");

        // Assert
        assertEquals("Juan Pérez", result);
    }

    @Test
    void buildFullNameShouldTrimWhitespaceFromBothNames() {
        // Act
        String result = UserNameUtils.buildFullName("  María  ", "  González  ");

        // Assert
        assertEquals("María González", result);
    }

    @Test
    void buildFullNameShouldReturnFirstNameWhenLastNameIsNull() {
        // Act
        String result = UserNameUtils.buildFullName("Carlos", null);

        // Assert
        assertEquals("Carlos", result);
    }

    @Test
    void buildFullNameShouldReturnLastNameWhenFirstNameIsNull() {
        // Act
        String result = UserNameUtils.buildFullName(null, "Rodríguez");

        // Assert
        assertEquals("Rodríguez", result);
    }

    @Test
    void buildFullNameShouldReturnEmptyStringWhenBothNamesAreNull() {
        // Act
        String result = UserNameUtils.buildFullName(null, null);

        // Assert
        assertEquals("", result);
    }

    @Test
    void buildFullNameShouldTrimFirstNameWhenLastNameIsNull() {
        // Act
        String result = UserNameUtils.buildFullName("  Ana  ", null);

        // Assert
        assertEquals("Ana", result);
    }

    @Test
    void buildFullNameShouldTrimLastNameWhenFirstNameIsNull() {
        // Act
        String result = UserNameUtils.buildFullName(null, "  López  ");

        // Assert
        assertEquals("López", result);
    }

    @Test
    void buildFullNameShouldHandleEmptyStrings() {
        // Act
        String result1 = UserNameUtils.buildFullName("", "Martínez");
        String result2 = UserNameUtils.buildFullName("Pedro", "");
        String result3 = UserNameUtils.buildFullName("", "");

        // Assert
        assertEquals("Martínez", result1);
        assertEquals("Pedro", result2);
        assertEquals("", result3);
    }

    @Test
    void buildFullNameShouldHandleWhitespaceOnlyStrings() {
        // Act
        String result1 = UserNameUtils.buildFullName("   ", "García");
        String result2 = UserNameUtils.buildFullName("Luis", "   ");
        String result3 = UserNameUtils.buildFullName("   ", "   ");

        // Assert
        assertEquals("García", result1);
        assertEquals("Luis", result2);
        assertEquals("", result3);
    }

    @ParameterizedTest
    @MethodSource("buildFullNameTestData")
    void buildFullNameShouldHandleVariousInputCombinations(String firstName, String lastName, String expected) {
        // Act
        String result = UserNameUtils.buildFullName(firstName, lastName);

        // Assert
        assertEquals(expected, result);
    }

    static Stream<Arguments> buildFullNameTestData() {
        return Stream.of(
                Arguments.of("José", "Martín", "José Martín"),
                Arguments.of("María Elena", "Fernández Silva", "María Elena Fernández Silva"),
                Arguments.of("A", "B", "A B"),
                Arguments.of("José María", null, "José María"),
                Arguments.of(null, "De la Cruz", "De la Cruz"),
                Arguments.of("  Sofía  ", "  Vargas  ", "Sofía Vargas"),
                Arguments.of("Miguel Ángel", "  ", "Miguel Ángel"),
                Arguments.of("  ", "Hernández", "Hernández"),
                Arguments.of("", null, ""),
                Arguments.of(null, "", "")
        );
    }

    @Test
    void buildFullNameShouldHandleSpecialCharacters() {
        // Act
        String result = UserNameUtils.buildFullName("José María", "Pérez-González");

        // Assert
        assertEquals("José María Pérez-González", result);
    }

    @Test
    void buildFullNameShouldHandleAccentedCharacters() {
        // Act
        String result = UserNameUtils.buildFullName("Andrés", "Muñoz");

        // Assert
        assertEquals("Andrés Muñoz", result);
    }
}
