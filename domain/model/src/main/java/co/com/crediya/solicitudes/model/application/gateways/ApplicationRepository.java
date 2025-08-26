package co.com.crediya.solicitudes.model.application.gateways;

import co.com.crediya.solicitudes.model.application.Application;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApplicationRepository {
    Mono<Application> save(Application application);
    Mono<Application> findById(Long id);
    Flux<Application> findByEmail(String email);
    Flux<Application> findByStatusId(Long statusId);
    Flux<Application> findAll();
    Mono<Void> deleteById(Long id);
}
