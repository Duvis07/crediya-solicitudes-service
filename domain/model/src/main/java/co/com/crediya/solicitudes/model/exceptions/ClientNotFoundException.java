package co.com.crediya.solicitudes.model.exceptions;

public class ClientNotFoundException extends RuntimeException {
    
    public ClientNotFoundException(String message) {
        super(message);
    }
}
