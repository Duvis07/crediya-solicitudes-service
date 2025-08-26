package co.com.crediya.solicitudes.model.utils;

import co.com.crediya.solicitudes.model.application.Application;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class ApplicationUtils {
    
    private ApplicationUtils() {}

    public static Mono<Application> enrichWithIds(Application application, Long loanTypeId, Long stateId) {
        LocalDateTime now = LocalDateTime.now();
        return Mono.just(application.toBuilder()
                .loanTypeId(loanTypeId)
                .stateId(stateId)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    public static Mono<Application> combineAndEnrich(Application application, Mono<Long> loanTypeId, Mono<Long> stateId) {
        return Mono.zip(loanTypeId, stateId)
                .flatMap(tuple -> enrichWithIds(application, tuple.getT1(), tuple.getT2()));
    }
}
