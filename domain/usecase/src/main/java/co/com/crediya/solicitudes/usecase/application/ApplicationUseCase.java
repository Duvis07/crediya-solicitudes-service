package co.com.crediya.solicitudes.usecase.application;

import co.com.crediya.solicitudes.model.application.gateways.ApplicationRepository;
import co.com.crediya.solicitudes.model.loantype.LoanType;
import co.com.crediya.solicitudes.model.loantype.gateways.LoanTypeRepository;
import co.com.crediya.solicitudes.model.loantype.LoanTypeEnum;
import co.com.crediya.solicitudes.model.state.gateways.StateRepository;
import co.com.crediya.solicitudes.model.state.State;
import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.exceptions.LoanTypeNotFoundException;
import co.com.crediya.solicitudes.model.utils.ApplicationUtils;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;


@RequiredArgsConstructor
public class ApplicationUseCase {

    private final ApplicationRepository applicationRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final StateRepository stateRepository;

    private static final String PENDING_STATUS = "Pendiente de revision";

    public Mono<Application> createApplication(Application application, LoanTypeEnum loanType) {
        return ApplicationUtils.combineAndEnrich(
                application, 
                getLoanTypeId(loanType), 
                getPendingStatus()
        ).flatMap(applicationRepository::save);
    }

    private Mono<Long> getLoanTypeId(LoanTypeEnum loanType) {
        return loanTypeRepository.findByName(loanType.getDisplayName())
                .map(LoanType::getLoanTypeId)
                .switchIfEmpty(Mono.error(new LoanTypeNotFoundException("Loan type not found: " + loanType.getDisplayName())));
    }

    private Mono<Long> getPendingStatus() {
        return stateRepository.findByName(PENDING_STATUS)
                .map(State::getStateId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Status 'Pendiente de revision' not found")));
    }

    public Flux<Application> getAllApplications() {
        return applicationRepository.findAll();
    }
}
