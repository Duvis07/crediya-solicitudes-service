package co.com.crediya.solicitudes.r2dbc.adapter;

import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.application.gateways.ApplicationRepository;
import co.com.crediya.solicitudes.r2dbc.repository.ApplicationEntityRepository;
import co.com.crediya.solicitudes.r2dbc.mapper.ApplicationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class ApplicationRepositoryAdapter implements ApplicationRepository {
    
    private final ApplicationEntityRepository applicationEntityRepository;
    private static final ApplicationMapper mapper = ApplicationMapper.INSTANCE;
    
    @Override
    public Mono<Application> save(Application application) {
        return Mono.just(application)
                .map(mapper::toEntity)
                .flatMap(applicationEntityRepository::save)
                .map(mapper::toDomain);
    }
    
    @Override
    public Mono<Application> findById(Long id) {
        return applicationEntityRepository.findById(id)
                .map(mapper::toDomain);
    }
    
    @Override
    public Flux<Application> findByEmail(String email) {
        return applicationEntityRepository.findByEmail(email)
                .map(mapper::toDomain);
    }
    
    @Override
    public Flux<Application> findByStatusId(Long statusId) {
        return applicationEntityRepository.findByStateId(statusId)
                .map(mapper::toDomain);
    }
    
    @Override
    public Flux<Application> findAll() {
        return applicationEntityRepository.findAll()
                .map(mapper::toDomain);
    }
    
    @Override
    public Mono<Void> deleteById(Long id) {
        return applicationEntityRepository.deleteById(id);
    }
}
