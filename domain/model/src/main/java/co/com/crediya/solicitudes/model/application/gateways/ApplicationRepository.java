package co.com.crediya.solicitudes.model.application.gateways;

import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.common.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ApplicationRepository {
    Mono<Application> save(Application application);
    Flux<Application> findByStateInWithPagination(List<Long> stateIds, PageRequest pageRequest);
    Mono<Long> countByStateIn(List<Long> stateIds);
    Flux<Application> findByDocumentIdAndStateId(String documentId, Long stateId);
}
