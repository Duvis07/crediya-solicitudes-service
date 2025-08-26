package co.com.crediya.solicitudes.api;

import co.com.crediya.solicitudes.api.dto.CreateApplicationRequest;
import co.com.crediya.solicitudes.api.exceptions.ValidationException;
import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.usecase.application.ApplicationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class Handler {
    
    private final ApplicationUseCase applicationUseCase;
    private final Validator validator;

    public Mono<ServerResponse> createApplication(ServerRequest request) {
        return request.bodyToMono(CreateApplicationRequest.class)
                .doOnNext(req -> log.info("Received application request for document: {}", req.getDocumentId()))
                .flatMap(this::validateRequest)
                .flatMap(req -> {
                    Application application = Application.builder()
                            .documentId(req.getDocumentId())
                            .email(req.getEmail())
                            .amount(req.getAmount())
                            .term(req.getTerm())
                            .build();
                    return applicationUseCase.createRequest(application, req.getLoanType());
                })
                .flatMap(application -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("Application created successfully"))
                .doOnSuccess(response -> log.info("Application created successfully"))
                .doOnError(error -> log.error("Error creating application: {}", error.getMessage()));
    }
    
    private Mono<CreateApplicationRequest> validateRequest(CreateApplicationRequest request) {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "createApplicationRequest");
        validator.validate(request, bindingResult);
        
        if (bindingResult.hasErrors()) {
            log.warn("Validation errors found: {}", bindingResult.getAllErrors());
            return Mono.error(new ValidationException("Validation failed", bindingResult));
        }
        
        return Mono.just(request);
    }
}
