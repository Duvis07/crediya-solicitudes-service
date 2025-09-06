package co.com.crediya.solicitudes.usecase.utils;

import co.com.crediya.solicitudes.model.state.ApplicationStatus;

import java.util.Set;

public final class ApplicationStateTransitionUtils {

    private ApplicationStateTransitionUtils() {}


    public static final Set<String> MANUAL_UPDATE_ALLOWED_STATES = Set.of(
            ApplicationStatus.PENDING_REVIEW.getDescription(),
            ApplicationStatus.MANUAL_REVIEW.getDescription()
    );


    public static final Set<String> VALID_TARGET_STATES = Set.of(
            ApplicationStatus.APPROVED.getDescription(),
            ApplicationStatus.REJECTED.getDescription()
    );

    public static boolean isValidForManualUpdate(String currentStateName) {
        return MANUAL_UPDATE_ALLOWED_STATES.contains(currentStateName);
    }

    public static boolean isValidTargetStatus(String targetStateName) {
        return VALID_TARGET_STATES.contains(targetStateName);
    }
}
