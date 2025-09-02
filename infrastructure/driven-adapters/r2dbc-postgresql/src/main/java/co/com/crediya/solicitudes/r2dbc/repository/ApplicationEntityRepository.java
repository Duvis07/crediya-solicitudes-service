package co.com.crediya.solicitudes.r2dbc.repository;

import co.com.crediya.solicitudes.r2dbc.entity.ApplicationEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface ApplicationEntityRepository extends ReactiveCrudRepository<ApplicationEntity, Long> {
    
    @Query("SELECT * FROM applications WHERE state_id IN (:stateIds) ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<ApplicationEntity> findByStateIdInWithPagination(List<Long> stateIds, int limit, long offset);
    
    @Query("SELECT COUNT(*) FROM applications WHERE state_id IN (:stateIds)")
    Mono<Long> countByStateIdIn(List<Long> stateIds);
    
    Flux<ApplicationEntity> findByDocumentIdAndStateId(String documentId, Long stateId);
}
