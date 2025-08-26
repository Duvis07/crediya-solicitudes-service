package co.com.crediya.solicitudes.model.state.gateways;

import co.com.crediya.solicitudes.model.state.State;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StateRepository {
    Mono<State> findById(Long id);
    Flux<State> findAll();
    Mono<State> findByName(String name);
}
