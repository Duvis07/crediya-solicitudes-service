package co.com.crediya.solicitudes.aws.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqsService {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    private static final String QUEUE_NAME = "solicitudes-capacidad-queue";
    private static final String QUEUE_URL = "http://localhost:4566/000000000000/solicitudes-capacidad-queue";

    /**
     * Envía una solicitud a la cola SQS para procesamiento de capacidad de endeudamiento
     */
    public Mono<String> enviarSolicitudParaEvaluacion(SolicitudCapacidadDto solicitudDto) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Enviando solicitud {} a cola SQS para evaluación automática", solicitudDto.getSolicitudId());
                
                String messageBody = objectMapper.writeValueAsString(solicitudDto);
                
                SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(QUEUE_URL)
                    .messageBody(messageBody)
                    .messageAttributes(Map.of(
                        "solicitudId", software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
                            .stringValue(solicitudDto.getSolicitudId())
                            .dataType("String")
                            .build(),
                        "documentoIdentidad", software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
                            .stringValue(solicitudDto.getDocumentoIdentidad())
                            .dataType("String")
                            .build(),
                        "tipoValidacion", software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
                            .stringValue("AUTOMATICA")
                            .dataType("String")
                            .build()
                    ))
                    .build();

                SendMessageResponse response = sqsClient.sendMessage(sendMessageRequest);
                
                log.info("Solicitud {} enviada exitosamente a SQS. MessageId: {}", 
                    solicitudDto.getSolicitudId(), response.messageId());
                
                return response.messageId();
                
            } catch (JsonProcessingException e) {
                log.error("Error serializando solicitud para SQS: {}", e.getMessage());
                throw new RuntimeException("Error enviando solicitud a cola SQS", e);
            } catch (Exception e) {
                log.error("Error enviando mensaje a SQS: {}", e.getMessage());
                throw new RuntimeException("Error de comunicación con SQS", e);
            }
        })
        .doOnSuccess(messageId -> log.info("Mensaje enviado a SQS con ID: {}", messageId))
        .doOnError(error -> log.error("Error enviando a SQS: {}", error.getMessage()));
    }

    /**
     * Envía notificación de resultado de evaluación
     */
    public Mono<String> enviarResultadoEvaluacion(ResultadoEvaluacionDto resultadoDto) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Enviando resultado de evaluación para solicitud {}", resultadoDto.getSolicitudId());
                
                String messageBody = objectMapper.writeValueAsString(resultadoDto);
                
                SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl("http://localhost:4566/000000000000/resultados-evaluacion-queue")
                    .messageBody(messageBody)
                    .messageAttributes(Map.of(
                        "solicitudId", software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
                            .stringValue(resultadoDto.getSolicitudId())
                            .dataType("String")
                            .build(),
                        "decision", software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
                            .stringValue(resultadoDto.getDecision())
                            .dataType("String")
                            .build()
                    ))
                    .build();

                SendMessageResponse response = sqsClient.sendMessage(sendMessageRequest);
                
                log.info("Resultado enviado exitosamente a SQS. MessageId: {}", response.messageId());
                
                return response.messageId();
                
            } catch (JsonProcessingException e) {
                log.error("Error serializando resultado para SQS: {}", e.getMessage());
                throw new RuntimeException("Error enviando resultado a cola SQS", e);
            } catch (Exception e) {
                log.error("Error enviando resultado a SQS: {}", e.getMessage());
                throw new RuntimeException("Error de comunicación con SQS", e);
            }
        });
    }

    /**
     * Crea las colas necesarias en LocalStack si no existen
     */
    public Mono<Void> inicializarColas() {
        return Mono.fromRunnable(() -> {
            try {
                log.info("Inicializando colas SQS en LocalStack...");
                
                // Crear cola de capacidad de endeudamiento
                sqsClient.createQueue(builder -> builder
                    .queueName("capacidad-endeudamiento-queue")
                    .build());
                
                // Crear cola de resultados
                sqsClient.createQueue(builder -> builder
                    .queueName("resultados-evaluacion-queue")
                    .build());
                
                log.info("Colas SQS inicializadas correctamente");
                
            } catch (Exception e) {
                log.warn("Error inicializando colas (pueden ya existir): {}", e.getMessage());
            }
        });
    }
}
