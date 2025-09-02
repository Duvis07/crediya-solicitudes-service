package co.com.crediya.solicitudes.api.service;

import co.com.crediya.solicitudes.api.dto.ApplicationDetailResponse;
import co.com.crediya.solicitudes.api.mapper.ApplicationDetailMapper;
import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.exceptions.UserServiceException;
import co.com.crediya.solicitudes.webclient.AuthServiceClient;
import co.com.crediya.solicitudes.webclient.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationDetailService {

    private final AuthServiceClient authServiceClient;
    private final ApplicationEnrichmentService enrichmentService;
    private final ApplicationDetailMapper applicationDetailMapper;

    public Mono<ApplicationDetailResponse> buildDetailResponse(Application application) {
        return authServiceClient.getUserByDocumentId(application.getDocumentId())
                .flatMap(user -> buildResponseWithUserData(application, user))
                .onErrorMap(error -> new UserServiceException(
                        "Failed to get user data for documentId: " + application.getDocumentId(), error));
    }

    private Mono<ApplicationDetailResponse> buildResponseWithUserData(Application application, UserResponse user) {
        Mono<String> stateMono = enrichmentService.getApplicationState(application.getStateId());
        Mono<String> loanTypeNameMono = enrichmentService.getLoanTypeName(application.getLoanTypeId());
        Mono<BigDecimal> interestRateMono = enrichmentService.getLoanInterestRate(application.getLoanTypeId());
        Mono<BigDecimal> totalDebtMono = enrichmentService.calculateTotalMonthlyDebt(application.getDocumentId());

        return Mono.zip(stateMono, loanTypeNameMono, interestRateMono, totalDebtMono)
                .map(tuple -> applicationDetailMapper.toDetailResponse(
                        application,
                        user,
                        tuple.getT1(), // applicationState
                        tuple.getT2(), // loanTypeName
                        tuple.getT3(), // interestRate
                        tuple.getT4()  // totalMonthlyDebt
                ));
    }
}
