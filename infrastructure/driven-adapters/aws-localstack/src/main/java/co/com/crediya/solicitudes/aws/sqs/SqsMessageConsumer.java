package co.com.crediya.solicitudes.aws.sqs;

import co.com.crediya.solicitudes.aws.dto.EvaluationResultDto;
import co.com.crediya.solicitudes.model.exceptions.MessageProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.com.crediya.solicitudes.aws.email.EmailNotificationService;
import co.com.crediya.solicitudes.model.application.gateways.CapacityEvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
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
                        .doOnSuccess(v -> deleteMessage(message))
                        .doOnError(error -> log.error("Error processing message {}: {}", 
                            message.messageId(), error.getMessage()))
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

    private Mono<Void> processEvaluationResult(EvaluationResultDto resultado) {
        return capacityEvaluationRepository.processEvaluationResult(
                resultado.getSolicitudId(),
                resultado.getDecision(),
                resultado.getMotivo(),
                resultado.getCapacidadDisponible(),
                resultado.getCuotaCalculada()
            )
            .flatMap(application -> {
                // Send email notification based on decision
                return switch (resultado.getDecision()) {
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
            })
            .doOnSuccess(v -> log.info("Result processed successfully for application: {}", 
                resultado.getSolicitudId()))
            .doOnError(error -> log.error("Error processing result for application {}: {}", 
                resultado.getSolicitudId(), error.getMessage()));
    }

    private void deleteMessage(Message message) {
        try {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                .queueUrl(RESULTS_QUEUE_URL)
                .receiptHandle(message.receiptHandle())
                .build();

            sqsClient.deleteMessage(deleteRequest);
            log.debug("Message {} deleted from queue", message.messageId());
            
        } catch (Exception e) {
            log.error("Error deleting message {}: {}", message.messageId(), e.getMessage());
        }
    }

}
