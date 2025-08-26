package co.com.crediya.solicitudes.model.application.gateways;

import co.com.crediya.solicitudes.model.application.Application;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApplicationRepository {
    Mono<Application> save(Application application);
    Flux<Application> findAll();
}
