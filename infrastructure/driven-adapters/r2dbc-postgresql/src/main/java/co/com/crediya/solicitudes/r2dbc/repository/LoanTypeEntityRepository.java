package co.com.crediya.solicitudes.r2dbc.repository;

import co.com.crediya.solicitudes.r2dbc.entity.LoanTypeEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface LoanTypeEntityRepository extends ReactiveCrudRepository<LoanTypeEntity, Long> {
    Mono<Boolean> existsById(Long id);
    Mono<LoanTypeEntity> findByName(String name);
}
