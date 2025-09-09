package co.com.crediya.solicitudes.model.exceptions;

public class SqsOperationException extends RuntimeException {
    
    public SqsOperationException(String message) {
        super(message);
    }
    
    public SqsOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
