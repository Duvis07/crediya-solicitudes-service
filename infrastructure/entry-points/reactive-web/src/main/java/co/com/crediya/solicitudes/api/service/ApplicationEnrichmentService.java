package co.com.crediya.solicitudes.api.service;

import co.com.crediya.solicitudes.api.utils.LoanCalculationUtils;
import co.com.crediya.solicitudes.model.application.gateways.ApplicationRepository;
import co.com.crediya.solicitudes.model.loantype.LoanType;
import co.com.crediya.solicitudes.model.loantype.gateways.LoanTypeRepository;
import co.com.crediya.solicitudes.model.state.ApplicationStatus;
import co.com.crediya.solicitudes.model.state.State;
import co.com.crediya.solicitudes.model.state.gateways.StateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationEnrichmentService {

    private final StateRepository stateRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final ApplicationRepository applicationRepository;


    public Mono<String> getApplicationState(Long stateId) {
        return stateRepository.findById(stateId)
                .map(State::getName);
    }

    public Mono<String> getLoanTypeName(Long loanTypeId) {
        return loanTypeRepository.findById(loanTypeId)
                .map(LoanType::getName);
    }

    public Mono<BigDecimal> getLoanInterestRate(Long loanTypeId) {
        return loanTypeRepository.findById(loanTypeId)
                .map(LoanType::getInterestRate);
    }

    public Mono<BigDecimal> calculateTotalMonthlyDebt(String documentId) {
        return stateRepository.findByName(ApplicationStatus.DISBURSED.getDescription())
                .map(State::getStateId)
                .flatMapMany(disbursedStateId -> 
                    applicationRepository.findByDocumentIdAndStateId(documentId, disbursedStateId))
                .map(LoanCalculationUtils::calculateMonthlyPayment)
                .doOnNext(payment -> log.info("Monthly payment: {}", payment))
                .reduce(BigDecimal.ZERO, LoanCalculationUtils::addMonthlyPayments)
                .doOnNext(total -> log.info("Total existing monthly debt for {}: {}", documentId, total))
                .defaultIfEmpty(BigDecimal.ZERO)
                .onErrorReturn(BigDecimal.ZERO);
    }
}
