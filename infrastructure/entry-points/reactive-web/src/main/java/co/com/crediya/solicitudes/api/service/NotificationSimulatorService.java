package co.com.crediya.solicitudes.api.service;

import co.com.crediya.solicitudes.model.application.Application;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class NotificationSimulatorService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void simulateEmailNotification(Application application, String newStatus, String comments) {
        log.info("📧 ========== SIMULATED EMAIL NOTIFICATION ==========");
        log.info("📧 Timestamp: {}", LocalDateTime.now().format(FORMATTER));
        log.info("📧 To: {}", application.getEmail());
        log.info("📧 Subject: Decisión sobre su solicitud de crédito #{}", application.getApplicationId());
        log.info("📧 Status: {}", newStatus);
        log.info("📧 Amount: ${:,.2f}", application.getAmount());
        log.info("📧 Term: {} meses", application.getTerm());
        
        String emailContent = buildEmailContent(application, newStatus, comments);
        log.info("📧 Content:\n{}", emailContent);
        
        // Simular envío a SQS (futuro)
        simulateSQSMessage(application, newStatus, comments);
        
        log.info("📧 ================================================");
    }

    private String buildEmailContent(Application application, String newStatus, String comments) {
        StringBuilder content = new StringBuilder();
        content.append("Estimado cliente,\n\n");
        content.append("Le informamos sobre el estado de su solicitud de crédito:\n\n");
        content.append("• Solicitud ID: ").append(application.getApplicationId()).append("\n");
        content.append("• Monto solicitado: $").append(String.format("%,.2f", application.getAmount())).append("\n");
        content.append("• Plazo: ").append(application.getTerm()).append(" meses\n");
        content.append("• Estado actual: ").append(newStatus).append("\n");
        
        if (comments != null && !comments.trim().isEmpty()) {
            content.append("• Comentarios del asesor: ").append(comments).append("\n");
        }
        
        content.append("\nFecha de actualización: ").append(LocalDateTime.now().format(FORMATTER)).append("\n\n");
        
        if ("Aprobada".equals(newStatus)) {
            content.append("¡Felicitaciones! Su solicitud ha sido aprobada.\n");
            content.append("En breve nos pondremos en contacto para continuar con el proceso de desembolso.\n");
        } else if ("Rechazada".equals(newStatus)) {
            content.append("Lamentamos informarle que su solicitud no ha sido aprobada en esta ocasión.\n");
            content.append("Puede contactarnos para más información sobre los motivos.\n");
        }
        
        content.append("\nGracias por confiar en Crediya.\n");
        content.append("Equipo de Créditos");
        
        return content.toString();
    }

    private void simulateSQSMessage(Application application, String newStatus, String comments) {
        log.info("🔔 ========== SIMULATED SQS MESSAGE ==========");
        log.info("🔔 Queue: crediya-loan-notifications");
        log.info("🔔 Message Type: LOAN_STATUS_UPDATE");
        log.info("🔔 Application ID: {}", application.getApplicationId());
        log.info("🔔 New Status: {}", newStatus);
        log.info("🔔 Client Email: {}", application.getEmail());
        log.info("🔔 Amount: ${:,.2f}", application.getAmount());
        
        // Simular estructura del mensaje JSON que se enviaría a SQS
        String messageBody = buildSQSMessageBody(application, newStatus, comments);
        log.info("🔔 Message Body: {}", messageBody);
        log.info("🔔 ==========================================");
    }

    private String buildSQSMessageBody(Application application, String newStatus, String comments) {
        return String.format("""
            {
              "messageType": "LOAN_STATUS_UPDATE",
              "timestamp": "%s",
              "applicationId": %d,
              "clientEmail": "%s",
              "newStatus": "%s",
              "amount": %.2f,
              "term": %d,
              "comments": "%s",
              "notificationChannel": "EMAIL"
            }
            """,
            LocalDateTime.now().format(FORMATTER),
            application.getApplicationId(),
            application.getEmail(),
            newStatus,
            application.getAmount(),
            application.getTerm(),
            comments != null ? comments : ""
        );
    }

    public void simulateSMSNotification(Application application, String newStatus) {
        log.info("📱 ========== SIMULATED SMS NOTIFICATION ==========");
        log.info("📱 To: Cliente (número no disponible en modelo actual)");
        log.info("📱 Message: Crediya: Su solicitud #{} ha sido {}. Revise su email para más detalles.",
                application.getApplicationId(), newStatus.toLowerCase());
        log.info("📱 ===============================================");
    }
}
