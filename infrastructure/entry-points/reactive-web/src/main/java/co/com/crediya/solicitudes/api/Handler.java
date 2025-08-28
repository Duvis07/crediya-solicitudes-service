package co.com.crediya.solicitudes.api;

import co.com.crediya.solicitudes.api.dto.CreateApplicationRequest;
import co.com.crediya.solicitudes.api.mapper.ApplicationDtoMapper;
import co.com.crediya.solicitudes.api.validator.RequestValidator;
import co.com.crediya.solicitudes.usecase.application.ApplicationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class Handler {
    
    private final ApplicationUseCase applicationUseCase;
    private final ApplicationDtoMapper applicationDtoMapper;
    private final RequestValidator requestValidator;

    public Mono<ServerResponse> createApplication(ServerRequest request) {
        return request.bodyToMono(CreateApplicationRequest.class)
                .doOnNext(req -> log.info("Received application request for document: {}", req.getDocumentId()))
                .flatMap(req -> requestValidator.validate(req, "createApplicationRequest"))
                .flatMap(validatedReq -> {
                    var application = applicationDtoMapper.toDomain(validatedReq);
                    return applicationUseCase.createApplication(application, validatedReq.getLoanType());
                })
                .flatMap(application -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("Application created successfully"))
                .doOnSuccess(response -> log.info("Application created successfully"))
                .doOnError(error -> log.error("Error creating application: {}", error.getMessage()));
    }
    
    public Mono<ServerResponse> getAllApplications(ServerRequest serverRequest) {
        return applicationUseCase.getAllApplications()
                .map(applicationDtoMapper::toResponse)
                .collectList()
                .flatMap(applications -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(applications))
                .onErrorResume(error -> {
                    log.error("Error retrieving applications: {}", error.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .bodyValue("Error retrieving applications");
                });
    }
}
