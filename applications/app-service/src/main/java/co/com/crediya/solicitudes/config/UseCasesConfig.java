package co.com.crediya.solicitudes.config;

import co.com.crediya.solicitudes.model.application.gateways.ApplicationRepository;
import co.com.crediya.solicitudes.model.loantype.gateways.LoanTypeRepository;
import co.com.crediya.solicitudes.model.state.gateways.StateRepository;
import co.com.crediya.solicitudes.usecase.application.ApplicationUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCasesConfig {

    @Bean
    public ApplicationUseCase applicationUseCase(ApplicationRepository applicationRepository,
                                               LoanTypeRepository loanTypeRepository,
                                               StateRepository stateRepository) {
        return new ApplicationUseCase(applicationRepository, loanTypeRepository, stateRepository);
    }
}
