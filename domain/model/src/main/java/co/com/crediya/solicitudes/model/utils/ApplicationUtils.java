package co.com.crediya.solicitudes.model.utils;

import co.com.crediya.solicitudes.model.application.Application;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

public class ApplicationUtils {

    private ApplicationUtils() {
    }

    public static Mono<Application> enrichWithIds(Application application, Long loanTypeId, Long stateId) {
        return Mono.fromCallable(() -> {
            LocalDateTime now = LocalDateTime.now();
            return application.toBuilder()
                    .loanTypeId(loanTypeId)
                    .stateId(stateId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public static Mono<Application> combineAndEnrich(Application application, Mono<Long> loanTypeId, Mono<Long> stateId) {
        return Mono.zip(loanTypeId, stateId)
                .flatMap(tuple -> enrichWithIds(application, tuple.getT1(), tuple.getT2()));
    }
}
