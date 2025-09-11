package co.com.crediya.solicitudes.api.mapper;

import co.com.crediya.solicitudes.api.dto.CalculateCapacityRequest;
import co.com.crediya.solicitudes.model.lambda.CapacityCalculationRequest;
import org.springframework.stereotype.Component;


@Component
public class CapacityCalculationMapper {


    public CapacityCalculationRequest mapToUseCaseRequest(CalculateCapacityRequest apiRequest) {
        return CapacityCalculationRequest.builder()
                .documentoIdentidad(apiRequest.getDocumentoIdentidad())
                .monto(apiRequest.getMonto())
                .plazoMeses(apiRequest.getPlazoMeses())
                .tasaInteresAnual(apiRequest.getTasaInteresAnual())
                .salarioBase(apiRequest.getSalarioBase())
                .email(apiRequest.getEmail())
                .build();
    }
}
