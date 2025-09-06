package co.com.crediya.solicitudes.model.exceptions;

public class StateNotFoundException extends RuntimeException {
    public StateNotFoundException(String message) {
        super(message);
    }
    
    public StateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
