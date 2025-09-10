package co.com.crediya.solicitudes.aws.email;

import co.com.crediya.solicitudes.aws.dto.ManualNotificationDto;
import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.exceptions.EmailNotificationException;
import co.com.crediya.solicitudes.model.exceptions.EmailTemplateException;
import co.com.crediya.solicitudes.model.exceptions.SqsOperationException;
import co.com.crediya.solicitudes.usecase.gateways.ManualNotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService implements ManualNotificationRepository {

    private final JavaMailSender mailSender;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${spring.mail.from:crediya@localhost}")
    private String fromEmail;

    @Value("${aws.sqs.notifications-queue-url:http://localhost:4566/000000000000/notificaciones-manuales-queue}")
    private String notificationsQueueUrl;

    private static final String UTF8_ENCODING = "UTF-8";
    private static final String NOTIFICATION_TYPE = "MANUAL_DECISION";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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

                        MimeMessage mimeMessage = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, UTF8_ENCODING);

                        helper.setFrom(fromEmail);
                        helper.setTo(email);
                        helper.setSubject("Solicitud en Revisión - CrediYa Financial");
                        helper.setText(htmlContent, true);

                        mailSender.send(mimeMessage);

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

                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, UTF8_ENCODING);

                helper.setFrom(fromEmail);
                helper.setTo(email);
                helper.setSubject("CrediYa - Loan Application Decision - Application " + solicitudId);
                helper.setText(htmlContent, true);

                mailSender.send(mimeMessage);

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

                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, UTF8_ENCODING);

                helper.setFrom(fromEmail);
                helper.setTo(email);
                helper.setSubject("CrediYa - Application Under Review - Application " + solicitudId);
                helper.setText(htmlContent, true);

                mailSender.send(mimeMessage);

                log.info("Manual review notification sent successfully to: {}", email);

            } catch (Exception e) {
                log.error("Error sending manual review notification to {}: {}", email, e.getMessage());
                throw new EmailNotificationException("Error sending manual review email", e);
            }
        });
    }

    /**
     * Loads HTML template from classpath resources
     */
    private String loadHtmlTemplate(String templatePath) {
        try {
            ClassPathResource resource = new ClassPathResource(templatePath);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error loading HTML template: {}", templatePath, e);
            throw new EmailTemplateException("Failed to load email template", e);
        }
    }

    /**
     * Processes approval email template with dynamic content
     */
    private String processApprovalTemplate(String htmlContent, String nombreCompleto, String solicitudId,
                                           BigDecimal montoAprobado, BigDecimal cuotaMensual,
                                           List<Map<String, Object>> planPagos) {
        // Replace basic placeholders
        htmlContent = htmlContent.replace("{{nombreCompleto}}", nombreCompleto != null ? nombreCompleto : "Cliente");
        htmlContent = htmlContent.replace("{{solicitudId}}", solicitudId != null ? solicitudId : "N/A");
        htmlContent = htmlContent.replace("{{montoAprobado}}", String.format("%.2f", montoAprobado));
        htmlContent = htmlContent.replace("{{cuotaMensual}}", String.format("%.2f", cuotaMensual));

        // Process payment schedule
        if (planPagos != null && !planPagos.isEmpty()) {
            StringBuilder scheduleRows = new StringBuilder();
            for (Map<String, Object> cuota : planPagos) {
                scheduleRows.append("<tr>")
                        .append("<td>").append(cuota.get("cuota")).append("</td>")
                        .append("<td>").append(cuota.get("fecha_vencimiento")).append("</td>")
                        .append("<td>$").append(String.format("%.2f", ((Number) cuota.get("capital")).doubleValue())).append("</td>")
                        .append("<td>$").append(String.format("%.2f", ((Number) cuota.get("interes")).doubleValue())).append("</td>")
                        .append("<td>$").append(String.format("%.2f", ((Number) cuota.get("saldo")).doubleValue())).append("</td>")
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
        htmlContent = htmlContent.replace("{{nombreCompleto}}", nombreCompleto != null ? nombreCompleto : "Cliente");
        htmlContent = htmlContent.replace("{{solicitudId}}", solicitudId != null ? solicitudId : "N/A");
        htmlContent = htmlContent.replace("{{motivo}}", motivo != null ? motivo : "Criterios de evaluación crediticia no cumplidos");
        // Clean up any remaining placeholders
        htmlContent = htmlContent.replaceAll("\\{\\{[^}]*\\}\\}", "");
        return htmlContent;
    }

    /**
     * Processes manual review email template with dynamic content
     */
    private String processManualReviewTemplate(String htmlContent, String nombreCompleto, String solicitudId) {
        htmlContent = htmlContent.replace("{{nombreCompleto}}", nombreCompleto != null ? nombreCompleto : "Cliente");
        htmlContent = htmlContent.replace("{{solicitudId}}", solicitudId != null ? solicitudId : "N/A");
        // Clean up any remaining placeholders
        htmlContent = htmlContent.replaceAll("\\{\\{[^}]*}}", "");
        return htmlContent;
    }

    // Implementation of ManualNotificationRepository
    @Override
    public Mono<Void> sendManualDecisionNotification(Long applicationId, String documentId, String email, 
                                                    String previousStatus, String newStatus, String comments, String reason) {
        log.info("Sending manual decision notification for application ID: {} with decision: {}", 
                applicationId, newStatus);

        return Mono.fromCallable(() -> buildNotificationDto(applicationId, documentId, email, previousStatus, newStatus, comments, reason))
                .flatMap(this::sendMessageToSqs)
                .doOnSuccess(v -> log.info("Manual decision notification sent successfully for application ID: {}", 
                        applicationId))
                .doOnError(error -> log.error("Failed to send manual decision notification for application ID: {}: {}", 
                        applicationId, error.getMessage()));
    }

    private ManualNotificationDto buildNotificationDto(Long applicationId, String documentId, String email,
                                                      String previousStatus, String newStatus, String comments, String reason) {
        String decision = mapStatusToDecision(newStatus);
        
        return ManualNotificationDto.builder()
                .applicationId(applicationId)
                .documentId(documentId)
                .customerName("Cliente")
                .email(email)
                .loanType("Préstamo Personal")
                .amount(0.0) // Will be filled by Lambda if needed
                .termMonths(0) // Will be filled by Lambda if needed
                .decision(decision)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .comments(comments)
                .reason(reason)
                .interestRate(12.5)
                .processedAt(LocalDateTime.now().format(FORMATTER))
                .notificationType(NOTIFICATION_TYPE)
                .build();
    }

    private String mapStatusToDecision(String status) {
        return switch (status.toLowerCase()) {
            case "approved", "aprobada" -> "APPROVED";
            case "rejected", "rechazada" -> "REJECTED";
            default -> status.toUpperCase();
        };
    }

    private Mono<Void> sendMessageToSqs(ManualNotificationDto notificationDto) {
        return Mono.fromCallable(() -> {
            try {
                String messageBody = objectMapper.writeValueAsString(notificationDto);
                log.debug("Sending SQS message: {}", messageBody);

                SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                        .queueUrl(notificationsQueueUrl)
                        .messageBody(messageBody)
                        .build();

                SendMessageResponse response = sqsClient.sendMessage(sendMessageRequest);
                log.info("SQS message sent successfully with MessageId: {}", response.messageId());
                return response;

            } catch (JsonProcessingException e) {
                log.error("Error serializing notification DTO to JSON: {}", e.getMessage());
                throw new SqsOperationException("Failed to serialize notification message", e);
            } catch (SqsException e) {
                log.error("Error sending message to SQS: {}", e.getMessage());
                throw new SqsOperationException("Failed to send message to SQS queue", e);
            }
        }).then();
    }

}
