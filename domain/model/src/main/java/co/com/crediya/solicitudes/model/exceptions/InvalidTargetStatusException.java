package co.com.crediya.solicitudes.model.exceptions;

public class InvalidTargetStatusException extends RuntimeException {
    
    public InvalidTargetStatusException(String status) {
        super(String.format("Invalid target status: %s. Only 'Approved' or 'Rejected' are allowed", status));
    }
}
