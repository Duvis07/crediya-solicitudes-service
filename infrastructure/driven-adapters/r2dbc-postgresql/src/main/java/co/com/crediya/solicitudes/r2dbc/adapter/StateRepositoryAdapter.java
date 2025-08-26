package co.com.crediya.solicitudes.r2dbc.adapter;

import co.com.crediya.solicitudes.model.state.State;
import co.com.crediya.solicitudes.model.state.gateways.StateRepository;
import co.com.crediya.solicitudes.r2dbc.entity.StateEntity;
import co.com.crediya.solicitudes.r2dbc.repository.StateEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class StateRepositoryAdapter implements StateRepository {
    
    private final StateEntityRepository stateEntityRepository;
    
    @Override
    public Mono<State> findById(Long id) {
        return stateEntityRepository.findById(id)
                .map(this::toDomain);
    }
    
    @Override
    public Flux<State> findAll() {
        return stateEntityRepository.findAll()
                .map(this::toDomain);
    }
    
    @Override
    public Mono<State> findByName(String name) {
        return stateEntityRepository.findByName(name)
                .map(this::toDomain);
    }
    
    private State toDomain(StateEntity entity) {
        return State.builder()
                .stateId(entity.getStateId())
                .name(entity.getName())
                .description(entity.getDescription())
                .build();
    }
}
