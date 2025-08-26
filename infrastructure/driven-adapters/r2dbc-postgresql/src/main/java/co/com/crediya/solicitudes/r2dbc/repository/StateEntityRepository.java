package co.com.crediya.solicitudes.r2dbc.repository;

import co.com.crediya.solicitudes.r2dbc.entity.StateEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface StateEntityRepository extends ReactiveCrudRepository<StateEntity, Long> {
    
    Mono<StateEntity> findByName(String name);
}
