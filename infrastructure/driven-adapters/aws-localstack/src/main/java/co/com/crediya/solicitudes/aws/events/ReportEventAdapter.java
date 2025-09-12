package co.com.crediya.solicitudes.aws.events;

import co.com.crediya.solicitudes.model.events.LoanApprovedEvent;
import co.com.crediya.solicitudes.model.events.gateways.ReportEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReportEventAdapter implements ReportEventRepository {

    private final SqsAsyncClient sqsAsyncClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.queues.loan-approved-events}")
    private String loanApprovedEventsQueue;

    @Override
    public Mono<Void> sendLoanApprovedEvent(LoanApprovedEvent event) {
        log.info("sending loan approved event to REPORTES - solicitud id: {}, email: {}, amount: {}",
                event.getSolicitudId(), event.getClientEmail(), event.getApprovedAmount());

        try {
            String messageBody = objectMapper.writeValueAsString(event);

            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(loanApprovedEventsQueue)
                    .messageBody(messageBody)
                    .build();

            return Mono.fromFuture(sqsAsyncClient.sendMessage(sendMessageRequest))
                    .doOnSuccess(response -> log.info(" loan approved event sent to reportes successfully - messageid: {}, queue: {}",
                            response.messageId(), loanApprovedEventsQueue))
                    .doOnError(error -> log.error(" error sending loan approved event to reportes for solicitud: {}",
                            event.getSolicitudId(), error))
                    .then();


        } catch (Exception e) {
            log.error("Error serializing loan approved event for solicitud: {}", event.getSolicitudId(), e);
            return Mono.error(e);
        }
    }
}
