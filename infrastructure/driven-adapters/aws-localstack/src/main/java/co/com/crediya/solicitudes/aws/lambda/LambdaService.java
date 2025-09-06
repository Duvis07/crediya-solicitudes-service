package co.com.crediya.solicitudes.aws.lambda;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.com.crediya.solicitudes.aws.sqs.SolicitudCapacidadDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LambdaService {

    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;

    private static final String LAMBDA_FUNCTION_NAME = "capacidad-endeudamiento-function";

    /**
     * Invoca la Lambda de capacidad de endeudamiento
     */
    public Mono<LambdaCapacidadResponse> evaluarCapacidadEndeudamiento(SolicitudCapacidadDto solicitudDto) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Invocando Lambda de capacidad de endeudamiento para solicitud: {}", 
                    solicitudDto.getSolicitudId());

                // Crear evento para la Lambda
                Map<String, Object> evento = Map.of(
                    "body", solicitudDto,
                    "httpMethod", "POST",
                    "headers", Map.of("Content-Type", "application/json")
                );

                String eventoJson = objectMapper.writeValueAsString(evento);
                
                InvokeRequest invokeRequest = InvokeRequest.builder()
                    .functionName(LAMBDA_FUNCTION_NAME)
                    .payload(SdkBytes.fromString(eventoJson, StandardCharsets.UTF_8))
                    .build();

                InvokeResponse response = lambdaClient.invoke(invokeRequest);
                
                String responsePayload = response.payload().asUtf8String();
                log.info("Respuesta de Lambda recibida para solicitud {}: {}", 
                    solicitudDto.getSolicitudId(), responsePayload);

                // Parsear respuesta
                LambdaCapacidadResponse lambdaResponse = objectMapper.readValue(
                    responsePayload, LambdaCapacidadResponse.class);

                return lambdaResponse;

            } catch (JsonProcessingException e) {
                log.error("Error procesando JSON para Lambda: {}", e.getMessage());
                throw new RuntimeException("Error en procesamiento JSON", e);
            } catch (Exception e) {
                log.error("Error invocando Lambda de capacidad: {}", e.getMessage());
                throw new RuntimeException("Error invocando Lambda", e);
            }
        })
        .doOnSuccess(response -> log.info("Lambda ejecutada exitosamente para solicitud: {}", 
            solicitudDto.getSolicitudId()))
        .doOnError(error -> log.error("Error en Lambda para solicitud {}: {}", 
            solicitudDto.getSolicitudId(), error.getMessage()));
    }

    /**
     * Verifica si la función Lambda existe y está disponible
     */
    public Mono<Boolean> verificarLambdaDisponible() {
        return Mono.fromCallable(() -> {
            try {
                log.info("Verificando disponibilidad de Lambda: {}", LAMBDA_FUNCTION_NAME);
                
                lambdaClient.getFunction(builder -> builder
                    .functionName(LAMBDA_FUNCTION_NAME)
                    .build());
                
                log.info("Lambda {} está disponible", LAMBDA_FUNCTION_NAME);
                return true;
                
            } catch (Exception e) {
                log.warn("Lambda {} no está disponible: {}", LAMBDA_FUNCTION_NAME, e.getMessage());
                return false;
            }
        });
    }
}
