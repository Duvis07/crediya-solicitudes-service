package co.com.crediya.solicitudes.aws.sqs;

import co.com.crediya.solicitudes.aws.dto.SolicitudCapacidadDto;
import co.com.crediya.solicitudes.model.exceptions.SqsOperationException;
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

    private static final String QUEUE_URL = "http://localhost:4566/000000000000/solicitudes-capacidad-queue";
    private static final String DATA_TYPE_STRING = "String";

    /**
     * Sends an application to SQS queue for debt capacity processing
     */
    public Mono<String> sendApplicationForEvaluation(SolicitudCapacidadDto solicitudDto) {
        return Mono.fromCallable(() -> {
                    try {
                        log.info("Sending application {} to SQS queue for automatic evaluation", solicitudDto.getSolicitudId());

                        String messageBody = objectMapper.writeValueAsString(solicitudDto);

                        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                                .queueUrl(QUEUE_URL)
                                .messageBody(messageBody)
                                .messageAttributes(Map.of(
                                        "solicitudId", software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
                                                .stringValue(solicitudDto.getSolicitudId())
                                                .dataType(DATA_TYPE_STRING)
                                                .build(),
                                        "documentoIdentidad", software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
                                                .stringValue(solicitudDto.getDocumentoIdentidad())
                                                .dataType(DATA_TYPE_STRING)
                                                .build(),
                                        "tipoValidacion", software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
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

}
