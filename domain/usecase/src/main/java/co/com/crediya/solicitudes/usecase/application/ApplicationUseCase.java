package co.com.crediya.solicitudes.usecase.application;

import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.application.gateways.ApplicationRepository;
import co.com.crediya.solicitudes.model.loantype.LoanType;
import co.com.crediya.solicitudes.model.loantype.gateways.LoanTypeRepository;
import co.com.crediya.solicitudes.model.loantype.LoanTypeEnum;
import co.com.crediya.solicitudes.model.state.gateways.StateRepository;
import co.com.crediya.solicitudes.model.state.State;
import co.com.crediya.solicitudes.model.exceptions.LoanTypeNotFoundException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class ApplicationUseCase {
    
    private final ApplicationRepository applicationRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final StateRepository stateRepository;
    
    private static final String PENDING_STATUS = "Pendiente de revision";
    
    public Mono<Application> createRequest(Application application, LoanTypeEnum loanType) {
        return getPendingStatus()
                .flatMap(statusId -> getLoanTypeId(loanType)
                        .flatMap(loanTypeId -> createNewRequest(application, loanTypeId, statusId)))
                .flatMap(applicationRepository::save);
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
    
    private Mono<Application> createNewRequest(Application application, Long loanTypeId, Long statusId) {
        return Mono.just(application.toBuilder()
                .loanTypeId(loanTypeId)
                .stateId(statusId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }
}
