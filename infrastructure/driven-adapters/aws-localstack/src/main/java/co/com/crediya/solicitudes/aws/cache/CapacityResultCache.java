package co.com.crediya.solicitudes.aws.cache;

import co.com.crediya.solicitudes.aws.cache.dto.CacheEntry;
import co.com.crediya.solicitudes.model.exceptions.CapacityCalculationInterruptedException;
import co.com.crediya.solicitudes.model.exceptions.CapacityCalculationTimeoutException;
import co.com.crediya.solicitudes.aws.dto.EvaluationResultDto;
import co.com.crediya.solicitudes.model.capacity.gateways.CapacityResultCacheGateway;
import co.com.crediya.solicitudes.model.lambda.CapacityCalculationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Slf4j
@Component
public class CapacityResultCache implements CapacityResultCacheGateway {

    private static final Duration CACHE_EXPIRY = Duration.ofMinutes(5);
    private static final Duration POLLING_INTERVAL = Duration.ofMillis(100);

    private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<>();


    public void storeResult(String messageId, EvaluationResultDto result) {
        if (messageId == null || result == null) {
            return;
        }

        CapacityCalculationResult capacityResult = mapToCapacityResult(result);
        storeResult(messageId, capacityResult);
    }

    @Override
    public void storeResult(String messageId, CapacityCalculationResult result) {
        if (messageId == null || result == null) {
            return;
        }

        CacheEntry entry = CacheEntry.builder()
                .result(result)
                .timestamp(LocalDateTime.now())
                .build();
        cache.put(messageId, entry);
        log.info("Stored capacity calculation result for messageId: {}", messageId);

        scheduleCleanup(messageId);
    }

    @Override
    public Mono<CapacityCalculationResult> waitForResult(String messageId, Duration timeout) {
        if (messageId == null || timeout == null) {
            return Mono.error(new IllegalArgumentException("MessageId and timeout cannot be null"));
        }

        return Mono.fromCallable(() -> waitForResultSync(messageId, timeout))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(RuntimeException.class, ex ->
                        new RuntimeException("Error waiting for capacity calculation result: " + ex.getMessage(), ex));
    }


    private CapacityCalculationResult waitForResultSync(String messageId, Duration timeout) {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeout.toMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            Optional<CacheEntry> entryOpt = Optional.ofNullable(cache.get(messageId));

            if (entryOpt.isPresent() && !entryOpt.get().isExpired(CACHE_EXPIRY)) {
                CacheEntry entry = entryOpt.get();
                cache.remove(messageId); // Remove after retrieval
                log.info("Retrieved capacity calculation result for messageId: {}", messageId);
                return entry.getResult();
            }

            try {
                Thread.sleep(POLLING_INTERVAL.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CapacityCalculationInterruptedException("Interrupted while waiting for result", e);
            }
        }

        log.warn("Timeout waiting for capacity calculation result for messageId: {}", messageId);
        throw new CapacityCalculationTimeoutException("Timeout waiting for capacity calculation result");
    }


    private void scheduleCleanup(String messageId) {
        Mono.delay(CACHE_EXPIRY)
                .doOnNext(ignored -> {
                    cache.remove(messageId);
                    log.debug("Expired cache entry for messageId: {}", messageId);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }


    private CapacityCalculationResult mapToCapacityResult(EvaluationResultDto dto) {
        if (dto == null) {
            return null;
        }

        return CapacityCalculationResult.builder()
                .decision(dto.getDecision())
                .motivo(dto.getMotivo())
                .capacidadDisponible(dto.getCapacidadDisponible())
                .cuotaCalculada(dto.getCuotaCalculada())
                .montoAprobado(dto.getMontoAprobado())
                .tasaInteresAnual(dto.getTasaInteresAnual())
                .plazoMeses(dto.getPlazoMeses())
                .cuotaMensual(dto.getCuotaMensual())
                .planPagos(dto.getPlanPagos() != null ?
                        new ArrayList<>(dto.getPlanPagos()) : null)
                .build();
    }

}
