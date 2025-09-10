package co.com.crediya.solicitudes.aws.sqs;

import co.com.crediya.solicitudes.aws.dto.CapacityRequestDto;
import co.com.crediya.solicitudes.model.exceptions.SqsOperationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqsService {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    private static final String CAPACITY_QUEUE_URL = "http://localhost:4566/000000000000/solicitudes-capacidad-queue";
    private static final String MANUAL_NOTIFICATIONS_QUEUE_URL = "http://localhost:4566/000000000000/notificaciones-manuales-queue";
    private static final String DATA_TYPE_STRING = "String";

    /**
     * Sends an application to SQS queue for debt capacity processing
     */
    public Mono<String> sendApplicationForEvaluation(CapacityRequestDto solicitudDto) {
        return Mono.fromCallable(() -> {
                    try {
                        log.info("Sending application {} to SQS queue for automatic evaluation", solicitudDto.getSolicitudId());

                        String messageBody = objectMapper.writeValueAsString(solicitudDto);

                        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                                .queueUrl(CAPACITY_QUEUE_URL)
                                .messageBody(messageBody)
                                .messageAttributes(Map.of(
                                        "solicitudId", MessageAttributeValue.builder()
                                                .stringValue(solicitudDto.getSolicitudId())
                                                .dataType(DATA_TYPE_STRING)
                                                .build(),
                                        "documentoIdentidad", MessageAttributeValue.builder()
                                                .stringValue(solicitudDto.getDocumentoIdentidad())
                                                .dataType(DATA_TYPE_STRING)
                                                .build(),
                                        "tipoValidacion", MessageAttributeValue.builder()
                                                .stringValue("AUTOMATICA")
                                                .dataType(DATA_TYPE_STRING)
                                                .build()
                                ))
                                .build();

                        SendMessageResponse response = sqsClient.sendMessage(sendMessageRequest);

                        log.info("Application {} sent successfully to SQS. MessageId: {}",
                                solicitudDto.getSolicitudId(), response.messageId());

                        return response.messageId();

                    } catch (JsonProcessingException e) {
                        log.error("Error serializing application for SQS: {}", e.getMessage());
                        throw new SqsOperationException("Error sending application to SQS queue", e);
                    } catch (Exception e) {
                        log.error("Error sending message to SQS: {}", e.getMessage());
                        throw new SqsOperationException("SQS communication error", e);
                    }
                })
                .doOnSuccess(messageId -> log.info("Message sent to SQS with ID: {}", messageId))
                .doOnError(error -> log.error("Error sending to SQS: {}", error.getMessage()));
    }

    /**
     * Sends manual notification to SQS queue with complete user data
     */
    public Mono<String> sendManualNotificationWithUserData(Long applicationId, String documentId, String email, 
                                                           String fullName, String newStatus, 
                                                           String comments, String reason) {
        return Mono.fromCallable(() -> {
                    try {
                        log.info("Sending manual notification for application {} to SQS queue", applicationId);
                        log.info("Input parameters: applicationId={}, documentId={}, email={}, fullName={}, newStatus={}", 
                                applicationId, documentId, email, fullName, newStatus);

                        String decision = mapStatusToDecision(newStatus);
                        log.info("Mapped decision: {} -> {}", newStatus, decision);
                        
                        Map<String, Object> notificationData = Map.of(
                                "solicitudId", applicationId.toString(),
                                "documentoIdentidad", documentId,
                                "email", email,
                                "nombreCompleto", fullName,
                                "decision", decision,
                                "comments", comments != null ? comments : "",
                                "reason", reason != null ? reason : "",
                                "isManualNotification", true
                        );

                        String messageBody = objectMapper.writeValueAsString(notificationData);
                        log.info("Generated message body: {}", messageBody);

                        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                                .queueUrl(MANUAL_NOTIFICATIONS_QUEUE_URL)
                                .messageBody(messageBody)
                                .messageAttributes(Map.of(
                                        "applicationId", MessageAttributeValue.builder()
                                                .stringValue(applicationId.toString())
                                                .dataType(DATA_TYPE_STRING)
                                                .build(),
                                        "notificationType", MessageAttributeValue.builder()
                                                .stringValue("MANUAL_DECISION")
                                                .dataType(DATA_TYPE_STRING)
                                                .build(),
                                        "newStatus", MessageAttributeValue.builder()
                                                .stringValue(newStatus)
                                                .dataType(DATA_TYPE_STRING)
                                                .build()
                                ))
                                .build();

                        SendMessageResponse response = sqsClient.sendMessage(sendMessageRequest);

                        log.info("Manual notification for application {} sent successfully to SQS. MessageId: {}",
                                applicationId, response.messageId());

                        return response.messageId();

                    } catch (JsonProcessingException e) {
                        log.error("Error serializing manual notification for SQS: {}", e.getMessage());
                        throw new SqsOperationException("Error sending manual notification to SQS queue", e);
                    } catch (Exception e) {
                        log.error("Error sending manual notification to SQS: {}", e.getMessage());
                        throw new SqsOperationException("SQS communication error for manual notification", e);
                    }
                })
                .doOnSuccess(messageId -> log.info("Manual notification sent to SQS with ID: {}", messageId))
                .doOnError(error -> log.error("Error sending manual notification to SQS: {}", error.getMessage()));
    }

    /**
     * Maps application status to decision format expected by SQS consumer
     */
    private String mapStatusToDecision(String status) {
        return switch (status.toLowerCase()) {
            case "aprobada", "approved" -> "APROBADA";
            case "rechazada", "rejected" -> "RECHAZADA";
            case "pendiente de revision", "revision manual", "manual review" -> "REVISION_MANUAL";
            default -> status.toUpperCase();
        };
    }

}
