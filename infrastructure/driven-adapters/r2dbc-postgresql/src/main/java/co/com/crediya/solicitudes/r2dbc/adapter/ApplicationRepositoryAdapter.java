package co.com.crediya.solicitudes.r2dbc.adapter;

import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.application.gateways.ApplicationRepository;
import co.com.crediya.solicitudes.r2dbc.repository.ApplicationEntityRepository;
import co.com.crediya.solicitudes.r2dbc.mapper.ApplicationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ApplicationRepositoryAdapter implements ApplicationRepository {

    private final ApplicationEntityRepository applicationEntityRepository;
    private final ApplicationMapper applicationMapper;

    @Override
    @Transactional
    public Mono<Application> save(Application application) {
        return Mono.just(application)
                .map(applicationMapper::toEntity)
                .flatMap(applicationEntityRepository::save)
                .map(applicationMapper::toDomain);
    }

    @Override
    public Flux<Application> findByStateInWithPagination(List<Long> stateIds, co.com.crediya.solicitudes.model.common.PageRequest pageRequest) {
        log.debug("Finding applications by states with pagination: states={}, page={}, size={}", 
                stateIds, pageRequest.page(), pageRequest.size());
        return applicationEntityRepository.findByStateIdInWithPagination(
                stateIds, 
                pageRequest.size(), 
                pageRequest.getOffset())
                .map(applicationMapper::toDomain);
    }

    @Override
    public Mono<Long> countByStateIn(List<Long> stateIds) {
        log.debug("Counting applications by states: {}", stateIds);
        return applicationEntityRepository.countByStateIdIn(stateIds);
    }

    @Override
    public Flux<Application> findByDocumentIdAndStateId(String documentId, Long stateId) {
        log.debug("Finding applications by documentId: {} and stateId: {}", documentId, stateId);
        return applicationEntityRepository.findByDocumentIdAndStateId(documentId, stateId)
                .map(applicationMapper::toDomain);
    }

    @Override
    public Mono<Application> findById(Long applicationId) {
        log.debug("Finding application by ID: {}", applicationId);
        return applicationEntityRepository.findById(applicationId)
                .map(applicationMapper::toDomain)
                .doOnSuccess(app -> log.debug("Found application: {}", applicationId))
                .doOnError(error -> log.error("Error finding application {}: {}", applicationId, error.getMessage()));
    }
}
