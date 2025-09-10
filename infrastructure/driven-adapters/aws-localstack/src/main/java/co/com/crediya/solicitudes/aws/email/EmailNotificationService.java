package co.com.crediya.solicitudes.aws.email;

import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.exceptions.EmailNotificationException;
import co.com.crediya.solicitudes.model.exceptions.EmailTemplateException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailNotificationService extends BaseEmailService {

    public EmailNotificationService(JavaMailSender mailSender) {
        super(mailSender);
    }

    private static final String TD_CLOSE = "</td>";
    private static final String TD_MONEY = "<td>$";

    /**
     * Sends approved payment plan notification via email using HTML template
     */
    public Mono<Void> sendPaymentPlanNotification(
            String email,
            String nombreCompleto,
            String solicitudId,
            BigDecimal montoAprobado,
            BigDecimal cuotaMensual,
            List<Map<String, Object>> planPagos) {

        return Mono.fromRunnable(() -> {
                    try {
                        log.info("Sending payment plan notification to: {} for application: {}", email, solicitudId);

                        String htmlContent = loadHtmlTemplate("email-templates/loan-approval.html");
                        htmlContent = processApprovalTemplate(htmlContent, nombreCompleto, solicitudId, montoAprobado, cuotaMensual, planPagos);

                        sendEmailSync(email, "¡Felicitaciones! Préstamo Aprobado - CREDIYA", htmlContent);

                        log.info("Payment plan notification sent successfully to: {}", email);

                    } catch (Exception e) {
                        log.error("Error sending payment plan notification to {}: {}", email, e.getMessage());
                        throw new EmailNotificationException("Error sending payment plan email", e);
                    }
                })
                .doOnSuccess(v -> log.info("Payment plan email processed for: {}", email))
                .doOnError(error -> log.error("Failed to send email: {}", error.getMessage())).then();
    }

    /**
     * Sends loan rejection notification via email
     */
    public Mono<Void> sendRejectionNotification(
            String email,
            String nombreCompleto,
            String solicitudId,
            String motivo) {

        return Mono.fromRunnable(() -> {
            try {
                log.info("Sending rejection notification to: {} for application: {}", email, solicitudId);

                String htmlContent = loadHtmlTemplate("email-templates/loan-rejection.html");
                htmlContent = processRejectionTemplate(htmlContent, nombreCompleto, solicitudId, motivo);

                sendEmailSync(email, "Actualización de su Solicitud - CREDIYA", htmlContent);

                log.info("Rejection notification sent successfully to: {}", email);

            } catch (Exception e) {
                log.error("Error sending rejection notification to {}: {}", email, e.getMessage());
                throw new EmailNotificationException("Error sending rejection email", e);
            }
        });
    }

    /**
     * Sends manual review notification via email
     */
    public Mono<Void> sendManualReviewNotification(
            String email,
            String nombreCompleto,
            String solicitudId) {

        return Mono.fromRunnable(() -> {
            try {
                log.info("Sending manual review notification to: {} for application: {}", email, solicitudId);

                String htmlContent = loadHtmlTemplate("email-templates/manual-review.html");
                htmlContent = processManualReviewTemplate(htmlContent, nombreCompleto, solicitudId);

                sendEmailSync(email, "Solicitud en Revisión Manual - CREDIYA", htmlContent);

                log.info("Manual review notification sent successfully to: {}", email);

            } catch (Exception e) {
                log.error("Error sending manual review notification to {}: {}", email, e.getMessage());
                throw new EmailNotificationException("Error sending manual review email", e);
            }
        });
    }


    /**
     * Processes approval email template with dynamic content
     */
    private String processApprovalTemplate(String htmlContent, String nombreCompleto, String solicitudId,
                                           BigDecimal montoAprobado, BigDecimal cuotaMensual,
                                           List<Map<String, Object>> planPagos) {
        // Replace basic placeholders
        htmlContent = htmlContent.replace(PLACEHOLDER_NOMBRE, nombreCompleto != null ? nombreCompleto : DEFAULT_CUSTOMER);
        htmlContent = htmlContent.replace(PLACEHOLDER_SOLICITUD, solicitudId != null ? solicitudId : "N/A");
        htmlContent = htmlContent.replace("{{montoAprobado}}", String.format("%.2f", montoAprobado));
        htmlContent = htmlContent.replace("{{cuotaMensual}}", String.format("%.2f", cuotaMensual));

        // Process payment schedule
        if (planPagos != null && !planPagos.isEmpty()) {
            StringBuilder scheduleRows = new StringBuilder();
            for (Map<String, Object> cuota : planPagos) {
                scheduleRows.append("<tr>")
                        .append("<td>").append(cuota.get("cuota")).append(TD_CLOSE)
                        .append("<td>").append(cuota.get("fecha_vencimiento")).append(TD_CLOSE)
                        .append(TD_MONEY).append(String.format("%.2f", ((Number) cuota.get("capital")).doubleValue())).append(TD_CLOSE)
                        .append(TD_MONEY).append(String.format("%.2f", ((Number) cuota.get("interes")).doubleValue())).append(TD_CLOSE)
                        .append(TD_MONEY).append(String.format("%.2f", ((Number) cuota.get("saldo")).doubleValue())).append(TD_CLOSE)
                        .append("</tr>");
            }
            // Replace the table body content with payment rows
            htmlContent = htmlContent.replace("<!-- Payment rows will be inserted here -->", scheduleRows.toString());
            // Keep the payment schedule section
            htmlContent = htmlContent.replace("{{#planPagos}}", "");
            htmlContent = htmlContent.replace("{{/planPagos}}", "");
            // Remove the fallback section completely
            htmlContent = htmlContent.replaceAll("\\{\\{\\^planPagos\\}\\}[\\s\\S]*?\\{\\{/planPagos\\}\\}", "");
        } else {
            // Remove payment schedule section and show fallback
            htmlContent = htmlContent.replaceAll("\\{\\{#planPagos\\}\\}[\\s\\S]*?\\{\\{/planPagos\\}\\}", "");
            htmlContent = htmlContent.replace("{{^planPagos}}", "");
            htmlContent = htmlContent.replace("{{/planPagos}}", "");
        }

        // Clean up any remaining placeholders
        htmlContent = htmlContent.replaceAll("\\{\\{[^}]*\\}\\}", "");

        return htmlContent;
    }

    /**
     * Processes rejection email template with dynamic content
     */
    private String processRejectionTemplate(String htmlContent, String nombreCompleto, String solicitudId, String motivo) {
        htmlContent = htmlContent.replace(PLACEHOLDER_NOMBRE, nombreCompleto != null ? nombreCompleto : DEFAULT_CUSTOMER);
        htmlContent = htmlContent.replace(PLACEHOLDER_SOLICITUD, solicitudId != null ? solicitudId : "N/A");
        htmlContent = htmlContent.replace("{{motivo}}", motivo != null ? motivo : "Criterios de evaluación crediticia no cumplidos");
        // Clean up any remaining placeholders
        htmlContent = htmlContent.replaceAll("\\{\\{[^}]*\\}\\}", "");
        return htmlContent;
    }

    /**
     * Processes manual review email template with dynamic content
     */
    private String processManualReviewTemplate(String htmlContent, String nombreCompleto, String solicitudId) {
        htmlContent = htmlContent.replace(PLACEHOLDER_NOMBRE, nombreCompleto != null ? nombreCompleto : DEFAULT_CUSTOMER);
        htmlContent = htmlContent.replace(PLACEHOLDER_SOLICITUD, solicitudId != null ? solicitudId : "N/A");
        // Clean up any remaining placeholders
        htmlContent = htmlContent.replaceAll(PLACEHOLDER_CLEANUP, "");
        return htmlContent;
    }


}
