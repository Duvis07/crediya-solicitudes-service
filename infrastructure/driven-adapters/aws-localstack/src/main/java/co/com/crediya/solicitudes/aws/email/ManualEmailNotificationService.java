package co.com.crediya.solicitudes.aws.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ManualEmailNotificationService extends BaseEmailService {

    public ManualEmailNotificationService(JavaMailSender mailSender) {
        super(mailSender);
    }

    public Mono<Void> sendManualApprovalNotification(String email, String customerName, String applicationId, String comments) {
        log.info("Sending manual approval notification to: {} for application: {}", email, applicationId);

        return loadEmailTemplate("manual-approval.html")
                .map(template -> processManualApprovalTemplate(template, customerName, applicationId, comments))
                .flatMap(message -> sendEmail(email, "¡Felicitaciones! Préstamo Aprobado - CREDIYA", message))
                .doOnSuccess(v -> log.info("Manual approval notification sent successfully to: {}", email))
                .doOnError(error -> log.error("Failed to send manual approval notification to {}: {}", email, error.getMessage()));
    }

    public Mono<Void> sendManualRejectionNotification(String email, String customerName, String applicationId, String reason) {
        log.info("Sending manual rejection notification to: {} for application: {}", email, applicationId);

        return loadEmailTemplate("manual-rejection.html")
                .map(template -> processManualRejectionTemplate(template, customerName, applicationId, reason))
                .flatMap(message -> sendEmail(email, "Actualización de su Solicitud - CREDIYA", message))
                .doOnSuccess(v -> log.info("Manual rejection notification sent successfully to: {}", email))
                .doOnError(error -> log.error("Failed to send manual rejection notification to {}: {}", email, error.getMessage()));
    }

    public Mono<Void> sendManualReviewNotification(String email, String customerName, String applicationId) {
        log.info("Sending manual review notification to: {} for application: {}", email, applicationId);

        return loadEmailTemplate("manual-review.html")
                .map(template -> processManualReviewTemplate(template, customerName, applicationId))
                .flatMap(message -> sendEmail(email, "Solicitud en Revisión Manual - CREDIYA", message))
                .doOnSuccess(v -> log.info("Manual review notification sent successfully to: {}", email))
                .doOnError(error -> log.error("Failed to send manual review notification to {}: {}", email, error.getMessage()));
    }


    private String processManualApprovalTemplate(String htmlContent, String nombreCompleto, String solicitudId, String comentarios) {
        htmlContent = htmlContent.replace(PLACEHOLDER_NOMBRE, nombreCompleto != null ? nombreCompleto : DEFAULT_CUSTOMER);
        htmlContent = htmlContent.replace(PLACEHOLDER_SOLICITUD, solicitudId != null ? solicitudId : "N/A");
        htmlContent = htmlContent.replace("{{comentarios}}", comentarios != null && !comentarios.isEmpty() ? comentarios : "Aprobación tras revisión manual");
        // Clean up any remaining placeholders
        htmlContent = htmlContent.replaceAll(PLACEHOLDER_CLEANUP, "");
        return htmlContent;
    }

    private String processManualRejectionTemplate(String htmlContent, String nombreCompleto, String solicitudId, String motivo) {
        htmlContent = htmlContent.replace(PLACEHOLDER_NOMBRE, nombreCompleto != null ? nombreCompleto : DEFAULT_CUSTOMER);
        htmlContent = htmlContent.replace(PLACEHOLDER_SOLICITUD, solicitudId != null ? solicitudId : "N/A");
        htmlContent = htmlContent.replace("{{motivo}}", motivo != null && !motivo.isEmpty() ? motivo : "Criterios de evaluación crediticia no cumplidos");
        // Clean up any remaining placeholders
        htmlContent = htmlContent.replaceAll(PLACEHOLDER_CLEANUP, "");
        return htmlContent;
    }

    private String processManualReviewTemplate(String htmlContent, String nombreCompleto, String solicitudId) {
        htmlContent = htmlContent.replace(PLACEHOLDER_NOMBRE, nombreCompleto != null ? nombreCompleto : DEFAULT_CUSTOMER);
        htmlContent = htmlContent.replace(PLACEHOLDER_SOLICITUD, solicitudId != null ? solicitudId : "N/A");
        // Clean up any remaining placeholders
        htmlContent = htmlContent.replaceAll(PLACEHOLDER_CLEANUP, "");
        return htmlContent;
    }

}
