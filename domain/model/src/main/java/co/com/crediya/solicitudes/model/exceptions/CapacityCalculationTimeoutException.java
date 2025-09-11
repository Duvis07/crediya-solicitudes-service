package co.com.crediya.solicitudes.model.exceptions;

/**
 * Exception thrown when capacity calculation waiting times out
 */
public class CapacityCalculationTimeoutException extends RuntimeException {
    
    public CapacityCalculationTimeoutException(String message) {
        super(message);
    }
    
    public CapacityCalculationTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
