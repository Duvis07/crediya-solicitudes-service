package co.com.crediya.solicitudes.model.exceptions;

public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
    
    public InvalidStateTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
