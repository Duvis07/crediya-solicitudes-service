package co.com.crediya.solicitudes.aws.sqs;

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
            log.info("Iniciando consumidor SQS para resultados de evaluación de capacidad");
            
            Mono.fromRunnable(this::consumeMessages)
                .subscribeOn(Schedulers.boundedElastic())
                .repeat()
                .delayElements(Duration.ofSeconds(5)) // Poll cada 5 segundos
                .doOnError(error -> {
                    log.error("Error en consumidor SQS: {}", error.getMessage());
                    isRunning.set(false);
                })
                .onErrorResume(error -> {
                    log.warn("Reintentando consumidor SQS después de error");
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
                .waitTimeSeconds(5) // Reducir long polling para evitar timeouts
                .messageAttributeNames("All")
                .build();

            ReceiveMessageResponse response = sqsClient.receiveMessage(receiveRequest);
            List<Message> messages = response.messages();

            if (!messages.isEmpty()) {
                log.info("Recibidos {} mensajes de resultados de evaluación", messages.size());
                
                for (Message message : messages) {
                    processMessage(message)
                        .doOnSuccess(v -> deleteMessage(message))
                        .doOnError(error -> log.error("Error procesando mensaje {}: {}", 
                            message.messageId(), error.getMessage()))
                        .onErrorResume(error -> Mono.empty()) // Continuar con otros mensajes
                        .block(); // Procesar secuencialmente
                }
            }

        } catch (Exception e) {
            log.error("Error consumiendo mensajes SQS: {}", e.getMessage());
            throw new RuntimeException("Error en consumidor SQS", e);
        }
    }

    private Mono<Void> processMessage(Message message) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Procesando mensaje de resultado: {}", message.messageId());
                
                String messageBody = message.body();
                log.info("Mensaje SQS crudo recibido: {}", messageBody);
                
                // Decodificar JSON que viene como string escapado
                try {
                    // Si el mensaje está envuelto en comillas, es un JSON escapado
                    if (messageBody.startsWith("\"") && messageBody.endsWith("\"")) {
                        // Usar ObjectMapper para decodificar el string JSON escapado
                        messageBody = objectMapper.readValue(messageBody, String.class);
                        log.info("JSON decodificado: {}", messageBody);
                    }
                } catch (Exception e) {
                    log.warn("No se pudo decodificar como JSON escapado, usando mensaje original: {}", e.getMessage());
                }
                
                ResultadoEvaluacionDto resultado = objectMapper.readValue(messageBody, ResultadoEvaluacionDto.class);
                
                log.info("Resultado recibido para solicitud {}: {}", 
                    resultado.getSolicitudId(), resultado.getDecision());
                
                return resultado;
                
            } catch (Exception e) {
                log.error("Error parseando mensaje: {}", e.getMessage());
                throw new RuntimeException("Error parseando resultado de evaluación", e);
            }
        })
        .flatMap(this::processEvaluationResult)
        .then();
    }

    private Mono<Void> processEvaluationResult(ResultadoEvaluacionDto resultado) {
        return capacityEvaluationRepository.procesarResultadoEvaluacion(
                resultado.getSolicitudId(),
                resultado.getDecision(),
                resultado.getMotivo(),
                resultado.getCapacidadDisponible(),
                resultado.getCuotaCalculada()
            )
            .flatMap(application -> {
                // Enviar notificación por email según la decisión
                return switch (resultado.getDecision()) {
                    case "APROBADO" -> emailNotificationService.enviarNotificacionPlanPagos(
                        resultado.getEmail(),
                        resultado.getNombreCompleto(),
                        resultado.getSolicitudId(),
                        application.getAmount(),
                        resultado.getCuotaCalculada(),
                        resultado.getPlanPagos()
                    );
                    case "RECHAZADO" -> emailNotificationService.enviarNotificacionRechazo(
                        resultado.getEmail(),
                        resultado.getNombreCompleto(),
                        resultado.getSolicitudId(),
                        resultado.getMotivo()
                    );
                    case "REVISION_MANUAL" -> emailNotificationService.enviarNotificacionRevisionManual(
                        resultado.getEmail(),
                        resultado.getNombreCompleto(),
                        resultado.getSolicitudId()
                    );
                    default -> {
                        log.warn("Decisión desconocida: {}", resultado.getDecision());
                        yield Mono.empty();
                    }
                };
            })
            .doOnSuccess(v -> log.info("Resultado procesado exitosamente para solicitud: {}", 
                resultado.getSolicitudId()))
            .doOnError(error -> log.error("Error procesando resultado para solicitud {}: {}", 
                resultado.getSolicitudId(), error.getMessage()));
    }

    private void deleteMessage(Message message) {
        try {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                .queueUrl(RESULTS_QUEUE_URL)
                .receiptHandle(message.receiptHandle())
                .build();

            sqsClient.deleteMessage(deleteRequest);
            log.debug("Mensaje {} eliminado de la cola", message.messageId());
            
        } catch (Exception e) {
            log.error("Error eliminando mensaje {}: {}", message.messageId(), e.getMessage());
        }
    }

    public void stopConsumer() {
        log.info("Deteniendo consumidor SQS");
        isRunning.set(false);
    }
}
