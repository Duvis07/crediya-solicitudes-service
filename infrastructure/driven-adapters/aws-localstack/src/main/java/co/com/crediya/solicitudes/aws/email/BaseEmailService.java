package co.com.crediya.solicitudes.aws.email;

import co.com.crediya.solicitudes.model.exceptions.EmailNotificationException;
import co.com.crediya.solicitudes.model.exceptions.EmailTemplateException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseEmailService {

    protected final JavaMailSender mailSender;

    @Value("${spring.mail.from:crediya@localhost}")
    protected String fromEmail;

    protected static final String UTF8_ENCODING = "UTF-8";
    protected static final String DEFAULT_CUSTOMER = "Cliente";
    protected static final String PLACEHOLDER_NOMBRE = "{{nombreCompleto}}";
    protected static final String PLACEHOLDER_SOLICITUD = "{{solicitudId}}";
    protected static final String PLACEHOLDER_CLEANUP = "\\{\\{[^}]*}}";

    /**
     * Loads HTML template from classpath resources (synchronous version)
     */
    protected String loadHtmlTemplate(String templatePath) {
        try {
            ClassPathResource resource = new ClassPathResource(templatePath);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load HTML template: {}", templatePath, e);
            throw new EmailTemplateException("Failed to load HTML template: " + templatePath, e);
        }
    }

    /**
     * Loads email template from classpath resources (reactive version)
     */
    protected Mono<String> loadEmailTemplate(String templateName) {
        return Mono.fromCallable(() -> {
            try {
                ClassPathResource resource = new ClassPathResource("email-templates/" + templateName);
                return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("Failed to load email template: {}", templateName, e);
                throw new EmailTemplateException("Failed to load email template: " + templateName, e);
            }
        });
    }

    /**
     * Sends email using JavaMailSender (reactive version)
     */
    protected Mono<Void> sendEmail(String to, String subject, String htmlContent) {
        return Mono.fromCallable(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, UTF8_ENCODING);
                
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);
                helper.setFrom(fromEmail);
                
                mailSender.send(message);
                log.info("Email sent successfully to: {}", to);
                return null;
            } catch (Exception e) {
                log.error("Failed to send email to {}: {}", to, e.getMessage());
                throw new EmailNotificationException("Failed to send email to: " + to, e);
            }
        });
    }

    /**
     * Sends email using JavaMailSender (synchronous version)
     */
    protected void sendEmailSync(String to, String subject, String htmlContent) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, UTF8_ENCODING);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Error sending email to {}: {}", to, e.getMessage());
            throw new EmailNotificationException("Error sending email", e);
        }
    }
}
