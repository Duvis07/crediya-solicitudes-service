package co.com.crediya.solicitudes.api.utils;

import co.com.crediya.solicitudes.model.common.PageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class PaginationUtilsTest {

    @Test
    void extractPaginationParamsShouldReturnDefaultValuesWhenNoParams() {
        // Arrange
        ServerRequest serverRequest = MockServerRequest.builder().build();

        // Act
        Mono<PageRequest> result = PaginationUtils.extractPaginationParams(serverRequest);

        // Assert
        StepVerifier.create(result)
                .assertNext(pageRequest -> {
                    assertEquals(0, pageRequest.page());
                    assertEquals(10, pageRequest.size());
                })
                .verifyComplete();
    }

    @Test
    void extractPaginationParamsShouldReturnCustomValuesWhenValidParams() {
        // Arrange
        ServerRequest serverRequest = MockServerRequest.builder()
                .queryParam("page", "2")
                .queryParam("size", "20")
                .build();

        // Act
        Mono<PageRequest> result = PaginationUtils.extractPaginationParams(serverRequest);

        // Assert
        StepVerifier.create(result)
                .assertNext(pageRequest -> {
                    assertEquals(2, pageRequest.page());
                    assertEquals(20, pageRequest.size());
                })
                .verifyComplete();
    }

    @Test
    void extractPaginationParamsShouldReturnErrorWhenPageIsNegative() {
        // Arrange
        ServerRequest serverRequest = MockServerRequest.builder()
                .queryParam("page", "-1")
                .queryParam("size", "10")
                .build();

        // Act
        Mono<PageRequest> result = PaginationUtils.extractPaginationParams(serverRequest);

        // Assert
        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void extractPaginationParamsShouldReturnErrorWhenSizeIsZero() {
        // Arrange
        ServerRequest serverRequest = MockServerRequest.builder()
                .queryParam("page", "0")
                .queryParam("size", "0")
                .build();

        // Act
        Mono<PageRequest> result = PaginationUtils.extractPaginationParams(serverRequest);

        // Assert
        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void extractPaginationParamsShouldReturnErrorWhenSizeIsNegative() {
        // Arrange
        ServerRequest serverRequest = MockServerRequest.builder()
                .queryParam("page", "0")
                .queryParam("size", "-5")
                .build();

        // Act
        Mono<PageRequest> result = PaginationUtils.extractPaginationParams(serverRequest);

        // Assert
        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void extractPaginationParamsShouldReturnErrorWhenSizeExceedsMaximum() {
        // Arrange
        ServerRequest serverRequest = MockServerRequest.builder()
                .queryParam("page", "0")
                .queryParam("size", "101")
                .build();

        // Act
        Mono<PageRequest> result = PaginationUtils.extractPaginationParams(serverRequest);

        // Assert
        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void extractPaginationParamsShouldReturnErrorWhenPageIsNotNumeric() {
        // Arrange
        ServerRequest serverRequest = MockServerRequest.builder()
                .queryParam("page", "abc")
                .queryParam("size", "10")
                .build();

        // Act
        Mono<PageRequest> result = PaginationUtils.extractPaginationParams(serverRequest);

        // Assert
        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void extractPaginationParamsShouldReturnErrorWhenSizeIsNotNumeric() {
        // Arrange
        ServerRequest serverRequest = MockServerRequest.builder()
                .queryParam("page", "0")
                .queryParam("size", "xyz")
                .build();

        // Act
        Mono<PageRequest> result = PaginationUtils.extractPaginationParams(serverRequest);

        // Assert
        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @ParameterizedTest
    @MethodSource("validPaginationParams")
    void extractPaginationParamsShouldReturnCorrectValuesForValidInputs(String page, String size, int expectedPage, int expectedSize) {
        // Arrange
        ServerRequest serverRequest = MockServerRequest.builder()
                .queryParam("page", page)
                .queryParam("size", size)
                .build();

        // Act
        Mono<PageRequest> result = PaginationUtils.extractPaginationParams(serverRequest);

        // Assert
        StepVerifier.create(result)
                .assertNext(pageRequest -> {
                    assertEquals(expectedPage, pageRequest.page());
                    assertEquals(expectedSize, pageRequest.size());
                })
                .verifyComplete();
    }

    static Stream<Arguments> validPaginationParams() {
        return Stream.of(
                Arguments.of("0", "1", 0, 1),
                Arguments.of("5", "25", 5, 25),
                Arguments.of("10", "50", 10, 50),
                Arguments.of("0", "100", 0, 100),
                Arguments.of("999", "99", 999, 99)
        );
    }

    @Test
    void extractPaginationParamsShouldUseDefaultWhenOnlyPageProvided() {
        // Arrange
        ServerRequest serverRequest = MockServerRequest.builder()
                .queryParam("page", "3")
                .build();

        // Act
        Mono<PageRequest> result = PaginationUtils.extractPaginationParams(serverRequest);

        // Assert
        StepVerifier.create(result)
                .assertNext(pageRequest -> {
                    assertEquals(3, pageRequest.page());
                    assertEquals(10, pageRequest.size()); // default size
                })
                .verifyComplete();
    }

    @Test
    void extractPaginationParamsShouldUseDefaultWhenOnlySizeProvided() {
        // Arrange
        ServerRequest serverRequest = MockServerRequest.builder()
                .queryParam("size", "15")
                .build();

        // Act
        Mono<PageRequest> result = PaginationUtils.extractPaginationParams(serverRequest);

        // Assert
        StepVerifier.create(result)
                .assertNext(pageRequest -> {
                    assertEquals(0, pageRequest.page()); // default page
                    assertEquals(15, pageRequest.size());
                })
                .verifyComplete();
    }
}
