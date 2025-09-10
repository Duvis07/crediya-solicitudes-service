package co.com.crediya.solicitudes.aws.sqs;

import co.com.crediya.solicitudes.aws.dto.EvaluationResultDto;
import co.com.crediya.solicitudes.model.exceptions.MessageProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.com.crediya.solicitudes.aws.email.EmailNotificationService;
import co.com.crediya.solicitudes.aws.email.ManualEmailNotificationService;
import co.com.crediya.solicitudes.model.application.gateways.CapacityEvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqsMessageConsumer {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final CapacityEvaluationRepository capacityEvaluationRepository;
    private final EmailNotificationService emailNotificationService;
    private final ManualEmailNotificationService manualEmailNotificationService;

    private static final String RESULTS_QUEUE_URL = "http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/resultados-evaluacion-queue";
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void startConsumer() {
        if (isRunning.compareAndSet(false, true)) {
            log.info("Starting SQS consumer for capacity evaluation results");

            Mono.fromRunnable(this::consumeMessages)
                    .subscribeOn(Schedulers.boundedElastic())
                    .repeat()
                    .delayElements(Duration.ofSeconds(5)) // Poll every 5 seconds
                    .doOnError(error -> {
                        log.error("Error in SQS consumer: {}", error.getMessage());
                        isRunning.set(false);
                    })
                    .onErrorResume(error -> {
                        log.warn("Retrying SQS consumer after error");
                        return Mono.delay(Duration.ofSeconds(10))
                                .then(Mono.fromRunnable(() -> isRunning.set(false)));
                    })
                    .subscribe();
        }
    }

    private void consumeMessages() {
        try {
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(RESULTS_QUEUE_URL)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(5) // Reduce long polling to avoid timeouts
                    .messageAttributeNames("All")
                    .build();

            ReceiveMessageResponse response = sqsClient.receiveMessage(receiveRequest);
            List<Message> messages = response.messages();

            if (!messages.isEmpty()) {
                log.info("Received {} evaluation result messages", messages.size());

                for (Message message : messages) {
                    processMessage(message)
                            .doOnSuccess(v -> {
                                log.info("Message {} processed successfully", message.messageId());
                                deleteMessage(message);
                            })
                            .doOnError(error -> log.error("Error processing message {}: {}", message.messageId(), error.getMessage()))
                            .onErrorResume(error -> Mono.empty()) // Continue with other messages
                            .block(); // Process sequentially
                }
            }

        } catch (Exception e) {
            log.error("Error consuming SQS messages: {}", e.getMessage());
            throw new MessageProcessingException("Error in SQS consumer", e);
        }
    }

    private Mono<Void> processMessage(Message message) {
        return Mono.fromCallable(() -> {
                    try {
                        log.info("Processing result message: {}", message.messageId());

                        String messageBody = message.body();
                        log.info("Raw SQS message received: {}", messageBody);

                        // Decode JSON that comes as escaped string
                        messageBody = decodeEscapedJson(messageBody);

                        EvaluationResultDto resultado = objectMapper.readValue(messageBody, EvaluationResultDto.class);

                        log.info("Result received for application {}: {}",
                                resultado.getSolicitudId(), resultado.getDecision());

                        return resultado;

                    } catch (Exception e) {
                        log.error("Error parsing message: {}", e.getMessage());
                        throw new MessageProcessingException("Error parsing evaluation result", e);
                    }
                })
                .flatMap(this::processEvaluationResult)
                .then();
    }

    private String decodeEscapedJson(String messageBody) {
        try {
            // If message is wrapped in quotes, it's escaped JSON
            if (messageBody.startsWith("\"") && messageBody.endsWith("\"")) {
                // Use ObjectMapper to decode the escaped JSON string
                messageBody = objectMapper.readValue(messageBody, String.class);
                log.info("Decoded JSON: {}", messageBody);
            }
        } catch (Exception e) {
            log.warn("Could not decode as escaped JSON, using original message: {}", e.getMessage());
        }
        return messageBody;
    }


    private Mono<Void> processEvaluationResult(EvaluationResultDto resultado) {
        // Check if this is a manual notification
        boolean isManualNotification = Boolean.TRUE.equals(resultado.getIsManualNotification());
        
        if (isManualNotification) {
            log.info("Processing manual notification for application: {}", resultado.getSolicitudId());
            return processManualNotification(resultado);
        } else {
            log.info("Processing automatic evaluation result for application: {}", resultado.getSolicitudId());
            return processAutomaticEvaluation(resultado);
        }
    }

    private Mono<Void> processManualNotification(EvaluationResultDto resultado) {
        // For manual notifications, we don't need to update capacity evaluation
        // Just send the email notification directly using specific manual templates
        Mono<Void> emailMono = switch (resultado.getDecision()) {
            case "APPROVED", "APROBADA" -> manualEmailNotificationService.sendManualApprovalNotification(
                    resultado.getEmail(),
                    resultado.getNombreCompleto(),
                    resultado.getSolicitudId(),
                    resultado.getComments() != null ? resultado.getComments() : "Aprobación tras revisión manual"
            );
            case "REJECTED", "RECHAZADA" -> manualEmailNotificationService.sendManualRejectionNotification(
                    resultado.getEmail(),
                    resultado.getNombreCompleto(),
                    resultado.getSolicitudId(),
                    resultado.getReason() != null ? resultado.getReason() : resultado.getComments()
            );
            default -> manualEmailNotificationService.sendManualReviewNotification(
                    resultado.getEmail(),
                    resultado.getNombreCompleto(),
                    resultado.getSolicitudId()
            );
        };

        return emailMono.retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(5))
                .doBeforeRetry(retrySignal ->
                        log.warn("Retrying manual notification email for application {}, attempt: {}",
                                resultado.getSolicitudId(), retrySignal.totalRetries() + 1))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.error("Manual notification email failed after {} retries for application {}",
                            retrySignal.totalRetries(), resultado.getSolicitudId());
                    return retrySignal.failure();
                }))
                .doOnSuccess(v -> log.info("Manual notification processed successfully for application: {}",
                        resultado.getSolicitudId()))
                .doOnError(error -> log.error("Error processing manual notification for application {}: {}",
                        resultado.getSolicitudId(), error.getMessage()));
    }

    private Mono<Void> processAutomaticEvaluation(EvaluationResultDto resultado) {
        return capacityEvaluationRepository.processEvaluationResult(
                        resultado.getSolicitudId(),
                        resultado.getDecision(),
                        resultado.getMotivo(),
                        resultado.getCapacidadDisponible(),
                        resultado.getCuotaCalculada()
                )
                .flatMap(application -> {
                    // Send email notification based on decision with retry logic
                    Mono<Void> emailMono = switch (resultado.getDecision()) {
                        case "APROBADO" -> emailNotificationService.sendPaymentPlanNotification(
                                resultado.getEmail(),
                                resultado.getNombreCompleto(),
                                resultado.getSolicitudId(),
                                application.getAmount(),
                                resultado.getCuotaCalculada(),
                                resultado.getPlanPagos()
                        );
                        case "RECHAZADO" -> emailNotificationService.sendRejectionNotification(
                                resultado.getEmail(),
                                resultado.getNombreCompleto(),
                                resultado.getSolicitudId(),
                                resultado.getMotivo()
                        );
                        case "REVISION_MANUAL" -> emailNotificationService.sendManualReviewNotification(
                                resultado.getEmail(),
                                resultado.getNombreCompleto(),
                                resultado.getSolicitudId()
                        );
                        default -> {
                            log.warn("Unknown decision: {}", resultado.getDecision());
                            yield Mono.empty();
                        }
                    };

                    return emailMono.retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(5))
                            .doBeforeRetry(retrySignal ->
                                    log.warn("Retrying email notification for application {}, attempt: {}",
                                            resultado.getSolicitudId(), retrySignal.totalRetries() + 1))
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                log.error("Email notification failed after {} retries for application {}",
                                        retrySignal.totalRetries(), resultado.getSolicitudId());
                                return retrySignal.failure();
                            }));
                })
                .doOnSuccess(v -> log.info("Automatic evaluation processed successfully for application: {}",
                        resultado.getSolicitudId()))
                .doOnError(error -> log.error("Error processing automatic evaluation for application {}: {}",
                        resultado.getSolicitudId(), error.getMessage()));
    }

    private void deleteMessage(Message message) {
        try {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(RESULTS_QUEUE_URL)
                    .receiptHandle(message.receiptHandle())
                    .build();

            sqsClient.deleteMessage(deleteRequest);
            log.info("Message {} deleted successfully from queue", message.messageId());

        } catch (Exception e) {
            log.error("Error deleting message {}: {}", message.messageId(), e.getMessage());
        }
    }


}
