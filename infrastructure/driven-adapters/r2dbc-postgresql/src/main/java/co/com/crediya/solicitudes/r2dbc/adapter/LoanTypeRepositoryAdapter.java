package co.com.crediya.solicitudes.r2dbc.adapter;

import co.com.crediya.solicitudes.model.loantype.LoanType;
import co.com.crediya.solicitudes.model.loantype.gateways.LoanTypeRepository;
import co.com.crediya.solicitudes.r2dbc.mapper.LoanTypeMapper;
import co.com.crediya.solicitudes.r2dbc.repository.LoanTypeEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class LoanTypeRepositoryAdapter implements LoanTypeRepository {

    private final LoanTypeEntityRepository loanTypeEntityRepository;
    private final LoanTypeMapper loanTypeMapper;

    @Override
    public Mono<LoanType> findByName(String name) {
        return loanTypeEntityRepository.findByName(name)
                .map(loanTypeMapper::toDomain);
    }

    @Override
    public Mono<LoanType> findById(Long loanTypeId) {
        return loanTypeEntityRepository.findById(loanTypeId)
                .map(loanTypeMapper::toDomain);
    }
}
