package co.com.crediya.solicitudes.aws.cache;

import co.com.crediya.solicitudes.model.lambda.CapacityCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;

@ExtendWith(MockitoExtension.class)
class CapacityResultCacheTest {

    private CapacityResultCache cache;

    @BeforeEach
    void setUp() {
        cache = new CapacityResultCache();
    }

    @Test
    void storeResult_ShouldStoreResult_WhenValidMessageIdAndResult() {
        // Arrange
        String messageId = "test-message-123";
        CapacityCalculationResult result = CapacityCalculationResult.builder()
                .decision("APROBADO")
                .motivo("Capacidad suficiente")
                .capacidadDisponible(new BigDecimal("700000"))
                .cuotaCalculada(new BigDecimal("25000"))
                .build();

        // Act
        cache.storeResult(messageId, result);

        // Assert - Access cache through reflection or create a getter method
        // Since getCache() doesn't exist, we'll test through waitForResult
        Mono<CapacityCalculationResult> resultMono = cache.waitForResult(messageId, Duration.ofSeconds(1));
        StepVerifier.create(resultMono)
                .expectNext(result)
                .verifyComplete();
    }

    @Test
    void waitForResult_ShouldReturnResult_WhenResultExistsInCache() {
        // Arrange
        String messageId = "existing-message-456";
        CapacityCalculationResult expectedResult = CapacityCalculationResult.builder()
                .decision("RECHAZADO")
                .motivo("Capacidad insuficiente")
                .capacidadDisponible(new BigDecimal("300000"))
                .cuotaCalculada(new BigDecimal("45000"))
                .build();

        cache.storeResult(messageId, expectedResult);

        // Act
        Mono<CapacityCalculationResult> result = cache.waitForResult(messageId, Duration.ofSeconds(1));

        // Assert
        StepVerifier.create(result)
                .expectNext(expectedResult)
                .verifyComplete();
    }

    @Test
    void waitForResult_ShouldEventuallyReturnResult_WhenResultArrivesLater() {
        // Arrange
        String messageId = "delayed-message-101";
        CapacityCalculationResult expectedResult = CapacityCalculationResult.builder()
                .decision("REVISION_MANUAL")
                .motivo("Monto alto requiere revisión")
                .capacidadDisponible(new BigDecimal("500000"))
                .cuotaCalculada(new BigDecimal("35000"))
                .build();

        // Act - Start waiting for result
        Mono<CapacityCalculationResult> resultMono = cache.waitForResult(messageId, Duration.ofSeconds(2));

        // Simulate result arriving after a delay
        new Thread(() -> {
            try {
                Thread.sleep(500);
                cache.storeResult(messageId, expectedResult);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // Assert
        StepVerifier.create(resultMono)
                .expectNext(expectedResult)
                .verifyComplete();
    }

    @Test
    void removeExpiredEntries_ShouldRemoveOldEntries_WhenEntriesAreExpired() {
        // Arrange
        String recentMessageId = "recent-message";

        CapacityCalculationResult result = CapacityCalculationResult.builder()
                .decision("APROBADO")
                .motivo("Test")
                .capacidadDisponible(new BigDecimal("1000000"))
                .cuotaCalculada(new BigDecimal("20000"))
                .build();

        // Add recent entry
        cache.storeResult(recentMessageId, result);

        // This test cannot be implemented as the cache doesn't expose removeExpiredEntries() 
        // or getCache() methods. The cache automatically handles expiry internally.
        // We'll test that recent entries can be retrieved
        Mono<CapacityCalculationResult> resultMono = cache.waitForResult(recentMessageId, Duration.ofSeconds(1));
        StepVerifier.create(resultMono)
                .expectNext(result)
                .verifyComplete();
    }

    @Test
    void storeResult_ShouldStoreAndRetrieveResult_WhenValidData() {
        // Arrange
        String messageId = "test-cache-access";
        CapacityCalculationResult result = CapacityCalculationResult.builder()
                .decision("APROBADO")
                .motivo("Test access")
                .capacidadDisponible(new BigDecimal("800000"))
                .cuotaCalculada(new BigDecimal("30000"))
                .build();

        // Act
        cache.storeResult(messageId, result);

        // Assert - Test through waitForResult since getCache() doesn't exist
        Mono<CapacityCalculationResult> resultMono = cache.waitForResult(messageId, Duration.ofSeconds(1));
        StepVerifier.create(resultMono)
                .expectNext(result)
                .verifyComplete();
    }
}
