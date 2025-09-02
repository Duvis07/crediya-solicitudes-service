package co.com.crediya.solicitudes.model.loantype.gateways;

import co.com.crediya.solicitudes.model.loantype.LoanType;
import reactor.core.publisher.Mono;

public interface LoanTypeRepository {
    Mono<LoanType> findByName(String name);
    Mono<LoanType> findById(Long loanTypeId);
}
