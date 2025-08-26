package co.com.crediya.solicitudes.r2dbc.repository;

import co.com.crediya.solicitudes.r2dbc.entity.ApplicationEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ApplicationEntityRepository extends ReactiveCrudRepository<ApplicationEntity, Long> {
    
    Flux<ApplicationEntity> findByEmail(String email);
    
    Flux<ApplicationEntity> findByStateId(Long stateId);
}
