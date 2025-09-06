package co.com.crediya.solicitudes.api.mapper;

import co.com.crediya.solicitudes.api.dto.ApplicationDetailResponse;
import co.com.crediya.solicitudes.api.utils.UserNameUtils;
import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.webclient.dto.UserResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ApplicationDetailMapper {

    public ApplicationDetailResponse toDetailResponse(
            Application application, 
            UserResponse user,
            String applicationState,
            String loanTypeName,
            BigDecimal interestRate,
            BigDecimal totalMonthlyDebt) {
        
        return ApplicationDetailResponse.builder()
                .applicationId(application.getApplicationId())
                .amount(application.getAmount())
                .term(application.getTerm())
                .applicationState(applicationState)
                .documentId(application.getDocumentId())
                .email(application.getEmail())
                .fullName(UserNameUtils.buildFullName(user.getFirstName(), user.getLastName()))
                .baseSalary(BigDecimal.valueOf(user.getBaseSalary()))
                .loanTypeName(loanTypeName)
                .interestRate(interestRate)
                .totalMonthlyDebt(totalMonthlyDebt)
                .build();
    }
}
