package co.com.crediya.solicitudes.usecase.application;

import co.com.crediya.solicitudes.model.application.gateways.ApplicationRepository;
import co.com.crediya.solicitudes.model.loantype.LoanType;
import co.com.crediya.solicitudes.model.loantype.gateways.LoanTypeRepository;
import co.com.crediya.solicitudes.model.loantype.LoanTypeEnum;
import co.com.crediya.solicitudes.model.state.gateways.StateRepository;
import co.com.crediya.solicitudes.model.state.State;
import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.state.ApplicationStatus;
import co.com.crediya.solicitudes.model.exceptions.LoanTypeNotFoundException;
import co.com.crediya.solicitudes.model.client.gateways.ClientValidationRepository;
import co.com.crediya.solicitudes.model.exceptions.ClientNotFoundException;
import co.com.crediya.solicitudes.model.common.PageRequest;
import co.com.crediya.solicitudes.model.common.PageResponse;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;
import java.util.logging.Logger;


@RequiredArgsConstructor
public class ApplicationUseCase {

    private final ApplicationRepository applicationRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final StateRepository stateRepository;
    private final ClientValidationRepository clientValidationRepository;

    private static final Logger log = Logger.getLogger(ApplicationUseCase.class.getName());

    public Mono<Application> createApplication(Application application, LoanTypeEnum loanType) {
        log.info("Creating application for client: " + application.getDocumentId());

        return validateClient(application)
                .then(Mono.zip(getLoanTypeId(loanType), getPendingStatus()))
                .map(tuple -> {
                    LocalDateTime now = LocalDateTime.now();
                    return application.toBuilder()
                            .loanTypeId(tuple.getT1())
                            .stateId(tuple.getT2())
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                })
                .flatMap(applicationRepository::save)
                .doOnSuccess(savedApp -> log.info("Application created successfully with ID: " + savedApp.getApplicationId()))
                .doOnError(error -> log.severe("Error creating application: " + error.getMessage()));
    }

    private Mono<Void> validateClient(Application application) {
        log.info("Validating client exists: " + application.getDocumentId());

        return clientValidationRepository.getUserEmailByDocumentId(application.getDocumentId())
                .switchIfEmpty(Mono.error(new ClientNotFoundException("Client not found with documentId: " + application.getDocumentId())))
                .flatMap(userEmail -> {
                    // Validate ownership - user can only create applications for themselves
                    if (!userEmail.equals(application.getEmail())) {
                        log.severe("OWNERSHIP DENIED: User with documentId " + application.getDocumentId() + 
                                " attempted to use incorrect email");
                        return Mono.error(new ClientNotFoundException(
                            "Access denied: You can only create loan applications for yourself. " +
                            "The email provided does not match the registered user."));
                    }
                    
                    log.info("Ownership validation passed for documentId: " + application.getDocumentId() +
                            " with email: " + userEmail);
                    return Mono.<Void>empty();
                })
                .doOnSuccess(v -> log.info("Client validation successful for: " + application.getDocumentId()))
                .doOnError(error -> log.severe("Client validation failed: " + error.getMessage()));
    }

    private Mono<Long> getLoanTypeId(LoanTypeEnum loanType) {
        log.info("Looking up loan type: " + loanType.getDisplayName());
        return loanTypeRepository.findByName(loanType.getDisplayName())
                .map(LoanType::getLoanTypeId)
                .doOnSuccess(id -> log.info("Found loan type ID: " + id))
                .switchIfEmpty(Mono.error(new LoanTypeNotFoundException("Loan type not found: " + loanType.getDisplayName())));
    }

    private Mono<Long> getPendingStatus() {
        return stateRepository.findByName(ApplicationStatus.PENDING_REVIEW.getDescription())
                .map(State::getStateId)
                .doOnSuccess(id -> log.info("Found pending status ID: " + id))
                .switchIfEmpty(Mono.error(new IllegalStateException("Status '" + ApplicationStatus.PENDING_REVIEW.getDescription() + "' not found")));
    }

    public Mono<PageResponse<Application>> getApplicationsForManualReviewPaginated(PageRequest pageRequest) {
        log.info("Getting paginated applications for manual review - page: " + pageRequest.page() + ", size: " + pageRequest.size());
        
        return getStateIdsForManualReview()
                .flatMap(stateIds -> {
                    log.info("Found state IDs for manual review: " + stateIds);
                    
                    Mono<List<Application>> contentMono = applicationRepository
                            .findByStateInWithPagination(stateIds, pageRequest)
                            .collectList();
                    
                    Mono<Long> totalMono = applicationRepository.countByStateIn(stateIds);
                    
                    return Mono.zip(contentMono, totalMono)
                            .map(tuple -> {
                                List<Application> content = tuple.getT1();
                                Long total = tuple.getT2();
                                log.info("Retrieved " + content.size() + " applications out of " + total + " total for manual review");
                                return PageResponse.of(content, pageRequest, total);
                            });
                })
                .doOnSuccess(page -> log.info("Successfully retrieved paginated applications for manual review"))
                .doOnError(error -> log.severe("Error retrieving paginated applications for manual review: " + error.getMessage()));
    }

    private Mono<List<Long>> getStateIdsForManualReview() {
        List<String> targetStates = Arrays.asList(
            ApplicationStatus.PENDING_REVIEW.getDescription(),
            ApplicationStatus.REJECTED.getDescription(),
            ApplicationStatus.MANUAL_REVIEW.getDescription()
        );
        
        return Flux.fromIterable(targetStates)
                .flatMap(stateName -> stateRepository.findByName(stateName)
                        .map(State::getStateId)
                        .doOnNext(id -> log.info("Found state ID " + id + " for state: " + stateName))
                        .onErrorResume(error -> {
                            log.severe("State not found: " + stateName + " - " + error.getMessage());
                            return Mono.empty();
                        }))
                .collectList()
                .filter(list -> !list.isEmpty())
                .switchIfEmpty(Mono.error(new IllegalStateException("No valid states found for manual review")));
    }
}
