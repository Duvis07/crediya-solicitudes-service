package co.com.crediya.solicitudes.model.loantype.gateways;

import co.com.crediya.solicitudes.model.loantype.LoanType;
import reactor.core.publisher.Mono;

public interface LoanTypeRepository {
    Mono<Boolean> existsById(Long id);
    Mono<LoanType> findById(Long id);
    Mono<LoanType> findByName(String name);
}
