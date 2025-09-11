package co.com.crediya.solicitudes.aws.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserNameUtilsTest {

    @Test
    void buildFullName_ShouldReturnFullName_WhenBothNamesProvided() {
        // Arrange
        String firstName = "Juan";
        String lastName = "Perez";

        // Act
        String result = UserNameUtils.buildFullName(firstName, lastName);

        // Assert
        assertEquals("Juan Perez", result);
    }

    @Test
    void buildFullName_ShouldReturnFirstName_WhenLastNameIsNull() {
        // Arrange
        String firstName = "Juan";
        String lastName = null;

        // Act
        String result = UserNameUtils.buildFullName(firstName, lastName);

        // Assert
        assertEquals("Juan", result);
    }

    @Test
    void buildFullName_ShouldReturnLastName_WhenFirstNameIsNull() {
        // Arrange
        String firstName = null;
        String lastName = "Perez";

        // Act
        String result = UserNameUtils.buildFullName(firstName, lastName);

        // Assert
        assertEquals("Perez", result);
    }

    @Test
    void buildFullName_ShouldReturnDefaultName_WhenBothNamesAreNull() {
        // Arrange
        String firstName = null;
        String lastName = null;

        // Act
        String result = UserNameUtils.buildFullName(firstName, lastName);

        // Assert
        assertEquals("Cliente", result);
    }

    @Test
    void buildFullName_ShouldTrimWhitespace_WhenNamesHaveExtraSpaces() {
        // Arrange
        String firstName = "  Juan  ";
        String lastName = "  Perez  ";

        // Act
        String result = UserNameUtils.buildFullName(firstName, lastName);

        // Assert
        assertEquals("Juan Perez", result);
    }

    @Test
    void buildFullName_ShouldHandleSpecialCharacters_WhenNamesContainAccents() {
        // Arrange
        String firstName = "José";
        String lastName = "García";

        // Act
        String result = UserNameUtils.buildFullName(firstName, lastName);

        // Assert
        assertEquals("José García", result);
    }

    @Test
    void buildFullName_ShouldHandleCompoundNames_WhenNamesHaveMultipleWords() {
        // Arrange
        String firstName = "María Elena";
        String lastName = "Rodríguez López";

        // Act
        String result = UserNameUtils.buildFullName(firstName, lastName);

        // Assert
        assertEquals("María Elena Rodríguez López", result);
    }

    @Test
    void buildFullName_ShouldHandleSingleCharacterNames() {
        // Arrange
        String firstName = "A";
        String lastName = "B";

        // Act
        String result = UserNameUtils.buildFullName(firstName, lastName);

        // Assert
        assertEquals("A B", result);
    }

    @Test
    void buildFullName_ShouldHandleLongNames() {
        // Arrange
        String firstName = "Juan Carlos Eduardo";
        String lastName = "Pérez García Rodríguez";

        // Act
        String result = UserNameUtils.buildFullName(firstName, lastName);

        // Assert
        assertEquals("Juan Carlos Eduardo Pérez García Rodríguez", result);
    }
}
