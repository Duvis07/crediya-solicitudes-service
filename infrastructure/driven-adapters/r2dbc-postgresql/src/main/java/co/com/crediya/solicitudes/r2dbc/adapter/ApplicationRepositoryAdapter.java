package co.com.crediya.solicitudes.r2dbc.adapter;

import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.application.gateways.ApplicationRepository;
import co.com.crediya.solicitudes.r2dbc.repository.ApplicationEntityRepository;
import co.com.crediya.solicitudes.r2dbc.mapper.ApplicationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
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
    public Flux<Application> findAll() {
        return applicationEntityRepository.findAll()
                .map(applicationMapper::toDomain);
    }
}
