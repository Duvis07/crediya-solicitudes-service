package co.com.crediya.solicitudes.model.capacity.gateways;

import co.com.crediya.solicitudes.model.lambda.CapacityCalculationResult;
import reactor.core.publisher.Mono;

import java.time.Duration;

public interface CapacityResultCacheGateway {

    void storeResult(String messageId, CapacityCalculationResult result);

    Mono<CapacityCalculationResult> waitForResult(String messageId, Duration timeout);

}
