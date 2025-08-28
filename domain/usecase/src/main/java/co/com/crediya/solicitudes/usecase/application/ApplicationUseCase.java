package co.com.crediya.solicitudes.usecase.application;

import co.com.crediya.solicitudes.model.application.gateways.ApplicationRepository;
import co.com.crediya.solicitudes.model.loantype.LoanType;
import co.com.crediya.solicitudes.model.loantype.gateways.LoanTypeRepository;
import co.com.crediya.solicitudes.model.loantype.LoanTypeEnum;
import co.com.crediya.solicitudes.model.state.gateways.StateRepository;
import co.com.crediya.solicitudes.model.state.State;
import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.exceptions.LoanTypeNotFoundException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.logging.Logger;


@RequiredArgsConstructor
public class ApplicationUseCase {

    private final ApplicationRepository applicationRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final StateRepository stateRepository;

    private static final Logger log = Logger.getLogger(ApplicationUseCase.class.getName());

    private static final String PENDING_STATUS = "Pendiente de revision";

    public Mono<Application> createApplication(Application application, LoanTypeEnum loanType) {
        return Mono.zip(getLoanTypeId(loanType), getPendingStatus())
                .map(tuple -> {
                    LocalDateTime now = LocalDateTime.now();
                    return application.toBuilder()
                            .loanTypeId(tuple.getT1())
                            .stateId(tuple.getT2())
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                })
                .flatMap(applicationRepository::save)
                .doOnSuccess(savedApp -> log.info("Application created successfully with ID: " + savedApp.getApplicationId()))
                .doOnError(error -> log.severe("Error creating application: " + error.getMessage()));
    }

    private Mono<Long> getLoanTypeId(LoanTypeEnum loanType) {
        log.info("Looking up loan type: " + loanType.getDisplayName());
        return loanTypeRepository.findByName(loanType.getDisplayName())
                .map(LoanType::getLoanTypeId)
                .doOnSuccess(id -> log.info("Found loan type ID: " + id))
                .switchIfEmpty(Mono.error(new LoanTypeNotFoundException("Loan type not found: " + loanType.getDisplayName())));
    }

    private Mono<Long> getPendingStatus() {
        log.info("Looking up pending status: " + PENDING_STATUS);
        return stateRepository.findByName(PENDING_STATUS)
                .map(State::getStateId)
                .doOnSuccess(id -> log.info("Found pending status ID: " + id))
                .switchIfEmpty(Mono.error(new IllegalStateException("Status 'Pendiente de revision' not found")));
    }

    public Flux<Application> getAllApplications() {
        return applicationRepository.findAll();
    }
}
