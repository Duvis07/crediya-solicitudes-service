package co.com.crediya.solicitudes.config;

import co.com.crediya.solicitudes.model.application.gateways.ApplicationRepository;
import co.com.crediya.solicitudes.model.application.gateways.CapacityEvaluationRepository;
import co.com.crediya.solicitudes.model.client.gateways.ClientValidationRepository;
import co.com.crediya.solicitudes.model.lambda.gateways.ManualNotificationRepository;
import co.com.crediya.solicitudes.model.loantype.gateways.LoanTypeRepository;
import co.com.crediya.solicitudes.model.state.gateways.StateRepository;
import co.com.crediya.solicitudes.usecase.application.ApplicationUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UseCasesConfigTest {

    @Test
    void testUseCaseBeansExist() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            ApplicationUseCase applicationUseCase = context.getBean(ApplicationUseCase.class);
            assertNotNull(applicationUseCase, "ApplicationUseCase bean should exist");

            String[] beanNames = context.getBeanDefinitionNames();
            boolean useCaseBeanFound = false;
            for (String beanName : beanNames) {
                if (beanName.endsWith("UseCase")) {
                    useCaseBeanFound = true;
                    break;
                }
            }

            assertTrue(useCaseBeanFound, "No beans ending with 'UseCase' were found");
        }
    }

    @Configuration
    @Import(UseCasesConfig.class)
    static class TestConfig {

        @Bean
        public ApplicationRepository applicationRepository() {
            return Mockito.mock(ApplicationRepository.class);
        }

        @Bean
        public LoanTypeRepository loanTypeRepository() {
            return Mockito.mock(LoanTypeRepository.class);
        }

        @Bean
        public StateRepository stateRepository() {
            return Mockito.mock(StateRepository.class);
        }

        @Bean
        public ClientValidationRepository clientValidationRepository() {
            return Mockito.mock(ClientValidationRepository.class);
        }

        @Bean
        public CapacityEvaluationRepository capacityEvaluationRepository() {
            return Mockito.mock(CapacityEvaluationRepository.class);
        }

        @Bean
        public ManualNotificationRepository manualNotificationRepository() {
            return Mockito.mock(ManualNotificationRepository.class);
        }

        @Bean
        public MyUseCase myUseCase() {
            return new MyUseCase();
        }
    }

    static class MyUseCase {
    }
}