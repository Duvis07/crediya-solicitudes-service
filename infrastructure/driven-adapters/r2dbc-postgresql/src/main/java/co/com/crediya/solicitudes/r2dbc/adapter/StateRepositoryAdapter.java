package co.com.crediya.solicitudes.r2dbc.adapter;

import co.com.crediya.solicitudes.model.state.State;
import co.com.crediya.solicitudes.model.state.gateways.StateRepository;
import co.com.crediya.solicitudes.r2dbc.mapper.StateMapper;
import co.com.crediya.solicitudes.r2dbc.repository.StateEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class StateRepositoryAdapter implements StateRepository {
    
    private final StateEntityRepository stateEntityRepository;
    private final StateMapper stateMapper;
    
    @Override
    public Mono<State> findByName(String name) {
        return stateEntityRepository.findByName(name)
                .map(stateMapper::toDomain);
    }

    @Override
    public Mono<State> findById(Long stateId) {
        return stateEntityRepository.findById(stateId)
                .map(stateMapper::toDomain);
    }

}
