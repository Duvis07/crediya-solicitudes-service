package co.com.crediya.solicitudes.model.exceptions;

/**
 * Exception thrown when email template operations fail
 */
public class EmailTemplateException extends RuntimeException {

    public EmailTemplateException(String message) {
        super(message);
    }

    public EmailTemplateException(String message, Throwable cause) {
        super(message, cause);
    }
}
