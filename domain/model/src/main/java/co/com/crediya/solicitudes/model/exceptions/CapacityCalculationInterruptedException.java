package co.com.crediya.solicitudes.model.exceptions;

/**
 * Exception thrown when capacity calculation waiting is interrupted
 */
public class CapacityCalculationInterruptedException extends RuntimeException {
    
    public CapacityCalculationInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CapacityCalculationInterruptedException(String message) {
        super(message);
    }
}
