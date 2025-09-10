package co.com.crediya.solicitudes.config;

import co.com.crediya.solicitudes.model.application.gateways.ApplicationRepository;
import co.com.crediya.solicitudes.model.application.gateways.CapacityEvaluationRepository;
import co.com.crediya.solicitudes.model.client.gateways.ClientValidationRepository;
import co.com.crediya.solicitudes.model.loantype.gateways.LoanTypeRepository;
import co.com.crediya.solicitudes.model.state.gateways.StateRepository;
import co.com.crediya.solicitudes.usecase.application.ApplicationUseCase;
import co.com.crediya.solicitudes.usecase.application.UpdateApplicationStatusUseCase;
import co.com.crediya.solicitudes.usecase.gateways.ManualNotificationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCasesConfig {

    @Bean
    public ApplicationUseCase applicationUseCase(ApplicationRepository applicationRepository,
                                                 LoanTypeRepository loanTypeRepository,
                                                 StateRepository stateRepository,
                                                 ClientValidationRepository clientValidationRepository,
                                                 CapacityEvaluationRepository capacityEvaluationRepository) {
        return new ApplicationUseCase(applicationRepository, loanTypeRepository, stateRepository, clientValidationRepository, capacityEvaluationRepository);
    }

    @Bean
    public UpdateApplicationStatusUseCase updateApplicationStatusUseCase(
            ApplicationRepository applicationRepository,
            StateRepository stateRepository,
            ManualNotificationRepository manualNotificationRepository) {
        return new UpdateApplicationStatusUseCase(applicationRepository, stateRepository, manualNotificationRepository);
    }
}

