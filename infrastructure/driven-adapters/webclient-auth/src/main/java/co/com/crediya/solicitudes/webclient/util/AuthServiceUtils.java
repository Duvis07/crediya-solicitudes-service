package co.com.crediya.solicitudes.webclient.util;

import co.com.crediya.solicitudes.model.exceptions.ClientNotFoundException;
import co.com.crediya.solicitudes.model.exceptions.ServiceUnavailableException;
import co.com.crediya.solicitudes.webclient.dto.UserResponse;
import co.com.crediya.solicitudes.model.client.UserType;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class AuthServiceUtils {

    private AuthServiceUtils() {
    }

    public static Mono<UserResponse> validateApplicantRole(UserResponse user, String documentId) {
        if (user.getUserType() == null || user.getUserType() != UserType.APPLICANT) {
            log.warn("ACCESS DENIED: User with documentId {} is not a CLIENTE. UserType: {}",
                    documentId, user.getUserType());
            return Mono.error(new ClientNotFoundException(
                    "Access denied: Only users with CLIENTE role can create loan applications. " +
                            "User type: " + user.getUserType()));
        }

        log.info("SUCCESS: User role validation passed for documentId: {} with userType: {}",
                documentId, user.getUserType());
        return Mono.just(user);
    }

    public static Throwable mapToBusinessException(Throwable ex, String documentId) {
        if (ex instanceof ClientNotFoundException) {
            return ex;
        }

        log.warn("FALLBACK: All retries failed for documentId: {}. Reason: {}", documentId, ex.getMessage());
        return new ServiceUnavailableException("Authentication service is temporarily unavailable. Please try again later.");
    }


    public static <T> Mono<T> handleNotFoundError(String documentId) {
        log.info("CLIENT NOT FOUND: documentId {} does not exist", documentId);
        return Mono.error(new ClientNotFoundException("Client not found with documentId: " + documentId));
    }
}
