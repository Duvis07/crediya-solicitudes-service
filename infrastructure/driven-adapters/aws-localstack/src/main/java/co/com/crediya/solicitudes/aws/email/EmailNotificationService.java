package co.com.crediya.solicitudes.aws.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:crediya@localhost}")
    private String fromEmail;

    /**
     * Envía notificación de plan de pagos aprobado por email
     */
    public Mono<Void> enviarNotificacionPlanPagos(
            String email,
            String nombreCompleto,
            String solicitudId,
            BigDecimal montoAprobado,
            BigDecimal cuotaMensual,
            List<Map<String, Object>> planPagos) {
        
        return Mono.fromRunnable(() -> {
            try {
                log.info("Enviando notificación de plan de pagos a: {} para solicitud: {}", email, solicitudId);
                
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(email);
                message.setSubject("CrediYa - Plan de Pagos Aprobado - Solicitud " + solicitudId);
                message.setText(construirMensajePlanPagos(nombreCompleto, solicitudId, montoAprobado, cuotaMensual, planPagos));
                
                mailSender.send(message);
                
                log.info("Notificación de plan de pagos enviada exitosamente a: {}", email);
                
            } catch (Exception e) {
                log.error("Error enviando notificación de plan de pagos a {}: {}", email, e.getMessage());
                throw new RuntimeException("Error enviando email de plan de pagos", e);
            }
        })
        .doOnSuccess(v -> log.info("Email de plan de pagos procesado para: {}", email))
        .doOnError(error -> log.error("Falló envío de email a {}: {}", email, error.getMessage())).then();
    }

    /**
     * Envía notificación de solicitud rechazada
     */
    public Mono<Void> enviarNotificacionRechazo(
            String email,
            String nombreCompleto,
            String solicitudId,
            String motivo) {
        
        return Mono.fromRunnable(() -> {
            try {
                log.info("Enviando notificación de rechazo a: {} para solicitud: {}", email, solicitudId);
                
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(email);
                message.setSubject("CrediYa - Solicitud de Crédito - Solicitud " + solicitudId);
                message.setText(construirMensajeRechazo(nombreCompleto, solicitudId, motivo));
                
                mailSender.send(message);
                
                log.info("Notificación de rechazo enviada exitosamente a: {}", email);
                
            } catch (Exception e) {
                log.error("Error enviando notificación de rechazo a {}: {}", email, e.getMessage());
                throw new RuntimeException("Error enviando email de rechazo", e);
            }
        });
    }

    /**
     * Envía notificación de revisión manual requerida
     */
    public Mono<Void> enviarNotificacionRevisionManual(
            String email,
            String nombreCompleto,
            String solicitudId) {
        
        return Mono.fromRunnable(() -> {
            try {
                log.info("Enviando notificación de revisión manual a: {} para solicitud: {}", email, solicitudId);
                
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(email);
                message.setSubject("CrediYa - Solicitud en Revisión - Solicitud " + solicitudId);
                message.setText(construirMensajeRevisionManual(nombreCompleto, solicitudId));
                
                mailSender.send(message);
                
                log.info("Notificación de revisión manual enviada exitosamente a: {}", email);
                
            } catch (Exception e) {
                log.error("Error enviando notificación de revisión manual a {}: {}", email, e.getMessage());
                throw new RuntimeException("Error enviando email de revisión manual", e);
            }
        });
    }

    private String construirMensajePlanPagos(
            String nombreCompleto,
            String solicitudId,
            BigDecimal montoAprobado,
            BigDecimal cuotaMensual,
            List<Map<String, Object>> planPagos) {
        
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Estimado/a ").append(nombreCompleto).append(",\n\n");
        mensaje.append("¡Felicitaciones! Su solicitud de crédito ha sido APROBADA.\n\n");
        mensaje.append("Detalles de su crédito:\n");
        mensaje.append("- Número de solicitud: ").append(solicitudId).append("\n");
        mensaje.append("- Monto aprobado: $").append(montoAprobado).append("\n");
        mensaje.append("- Cuota mensual: $").append(cuotaMensual).append("\n\n");
        
        mensaje.append("Plan de Pagos:\n");
        mensaje.append("Cuota | Fecha Vencimiento | Capital | Interés | Saldo\n");
        mensaje.append("------|------------------|---------|---------|-------\n");
        
        if (planPagos != null && !planPagos.isEmpty()) {
            for (Map<String, Object> cuota : planPagos) {
                mensaje.append(String.format("%5s | %16s | %7s | %7s | %7s\n",
                    cuota.get("cuota"),
                    cuota.get("fecha_vencimiento"),
                    String.format("%.2f", cuota.get("capital")),
                    String.format("%.2f", cuota.get("interes")),
                    String.format("%.2f", cuota.get("saldo"))
                ));
            }
        } else {
            mensaje.append("Plan de pagos detallado será proporcionado por su asesor.\n");
        }
        
        mensaje.append("\n\nPróximos pasos:\n");
        mensaje.append("1. Un asesor se contactará con usted en las próximas 24 horas\n");
        mensaje.append("2. Deberá presentar la documentación requerida\n");
        mensaje.append("3. Una vez completado el proceso, se realizará el desembolso\n\n");
        mensaje.append("Gracias por confiar en CrediYa.\n\n");
        mensaje.append("Atentamente,\n");
        mensaje.append("Equipo CrediYa");
        
        return mensaje.toString();
    }

    private String construirMensajeRechazo(String nombreCompleto, String solicitudId, String motivo) {
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Estimado/a ").append(nombreCompleto).append(",\n\n");
        mensaje.append("Lamentamos informarle que su solicitud de crédito ha sido RECHAZADA.\n\n");
        mensaje.append("Detalles:\n");
        mensaje.append("- Número de solicitud: ").append(solicitudId).append("\n");
        mensaje.append("- Motivo: ").append(motivo).append("\n\n");
        mensaje.append("Puede volver a aplicar en 30 días o contactar a nuestros asesores para más información.\n\n");
        mensaje.append("Gracias por su interés en CrediYa.\n\n");
        mensaje.append("Atentamente,\n");
        mensaje.append("Equipo CrediYa");
        
        return mensaje.toString();
    }

    private String construirMensajeRevisionManual(String nombreCompleto, String solicitudId) {
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Estimado/a ").append(nombreCompleto).append(",\n\n");
        mensaje.append("Su solicitud de crédito está en proceso de REVISIÓN MANUAL.\n\n");
        mensaje.append("Detalles:\n");
        mensaje.append("- Número de solicitud: ").append(solicitudId).append("\n\n");
        mensaje.append("Nuestro equipo de asesores revisará su solicitud y se contactará con usted ");
        mensaje.append("en un plazo máximo de 48 horas hábiles.\n\n");
        mensaje.append("Gracias por su paciencia.\n\n");
        mensaje.append("Atentamente,\n");
        mensaje.append("Equipo CrediYa");
        
        return mensaje.toString();
    }
}
