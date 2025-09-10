package co.com.crediya.solicitudes.usecase.application;

import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.application.UpdateApplicationStatusResult;
import co.com.crediya.solicitudes.model.application.gateways.ApplicationRepository;
import co.com.crediya.solicitudes.model.exceptions.ApplicationNotFoundException;
import co.com.crediya.solicitudes.model.exceptions.InvalidStateTransitionException;
import co.com.crediya.solicitudes.model.exceptions.StateNotFoundException;
import co.com.crediya.solicitudes.model.state.State;
import co.com.crediya.solicitudes.model.state.gateways.StateRepository;
import co.com.crediya.solicitudes.usecase.gateways.ManualNotificationRepository;
import co.com.crediya.solicitudes.usecase.utils.ApplicationStateTransitionUtils;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class UpdateApplicationStatusUseCase {

    private final ApplicationRepository applicationRepository;
    private final StateRepository stateRepository;
    private final ManualNotificationRepository manualNotificationRepository;

    private static final Logger log = Logger.getLogger(UpdateApplicationStatusUseCase.class.getName());

    public Mono<UpdateApplicationStatusResult> updateApplicationStatus(Long applicationId, String newStatus) {
        return updateApplicationStatus(applicationId, newStatus, null);
    }

    public Mono<UpdateApplicationStatusResult> updateApplicationStatus(Long applicationId, String newStatus, String comments) {

        return applicationRepository.findById(applicationId)
                .switchIfEmpty(Mono.error(new ApplicationNotFoundException("Application not found with ID: " + applicationId)))
                .doOnNext(app -> log.info("Found application with current state ID: " + app.getStateId()))
                .flatMap(application ->
                        stateRepository.findById(application.getStateId())
                                .flatMap(previousState -> {
                                    log.info("Current application state: " + previousState.getName());

                                    if (!ApplicationStateTransitionUtils.isValidForManualUpdate(previousState.getName())) {
                                        log.severe("Invalid state transition attempted from: " + previousState.getName());
                                        return Mono.error(new InvalidStateTransitionException(
                                                "Application cannot be modified in current state. Only applications in manual review can be approved/rejected."));
                                    }
                                    if (!ApplicationStateTransitionUtils.isValidTargetStatus(newStatus)) {
                                        log.severe("Invalid target status provided: " + newStatus);
                                        return Mono.error(new InvalidStateTransitionException(
                                                "Invalid status: " + newStatus + ". Only 'Approved' or 'Rejected' are allowed"));
                                    }

                                    log.info("State transition validation passed. Proceeding with update.");

                                    return getStateByName(newStatus)
                                            .flatMap(newState -> {
                                                log.info("Found target state ID: " + newState.getStateId() + " for status: " + newStatus);
                                                Application updatedApplication = updateApplicationWithNewState(application, newState);
                                                return applicationRepository.save(updatedApplication)
                                                        .flatMap(savedApp -> {
                                                            log.info("Application status updated successfully from '" +
                                                                    previousState.getName() + "' to '" + newState.getName() + "'");
                                                            
                                                            // Send SQS notification for manual decision
                                                            return manualNotificationRepository.sendManualDecisionNotification(
                                                                    savedApp.getApplicationId(), savedApp.getDocumentId(), savedApp.getEmail(),
                                                                    previousState.getName(), newState.getName(), comments, newState.getName())
                                                                    .then(Mono.just(new UpdateApplicationStatusResult(
                                                                            savedApp,
                                                                            previousState.getName(),
                                                                            newState.getName()
                                                                    )))
                                                                    .doOnSuccess(result -> log.info("Manual decision notification sent for application ID: " + applicationId))
                                                                    .onErrorResume(notificationError -> {
                                                                        log.severe("Failed to send manual decision notification: " + notificationError.getMessage());
                                                                        // Continue with the result even if notification fails
                                                                        return Mono.just(new UpdateApplicationStatusResult(
                                                                                savedApp,
                                                                                previousState.getName(),
                                                                                newState.getName()
                                                                        ));
                                                                    });
                                                        });
                                            });
                                })
                )
                .doOnError(error -> log.severe("Error updating application status: " + error.getMessage()));
    }


    private Mono<State> getStateByName(String stateName) {
        return stateRepository.findByName(stateName)
                .doOnNext(state -> log.info("Found state with ID: " + state.getStateId()))
                .switchIfEmpty(Mono.error(new StateNotFoundException(
                        String.format("State not found with name: %s", stateName))));
    }

    private Application updateApplicationWithNewState(Application application, State newState) {
        return application.toBuilder()
                .stateId(newState.getStateId())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
