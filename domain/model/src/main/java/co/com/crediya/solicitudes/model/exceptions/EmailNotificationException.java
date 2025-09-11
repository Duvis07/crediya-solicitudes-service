package co.com.crediya.solicitudes.model.exceptions;

/**
 * Exception thrown when email notification operations fail
 */
public class EmailNotificationException extends RuntimeException {

    public EmailNotificationException(String message) {
        super(message);
    }

    public EmailNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
