package co.com.crediya.solicitudes.api;

import co.com.crediya.solicitudes.api.dto.ApplicationCreatedResponse;
import co.com.crediya.solicitudes.api.dto.CreateApplicationRequest;
import co.com.crediya.solicitudes.api.dto.UpdateApplicationStatusRequest;
import co.com.crediya.solicitudes.api.mapper.ApplicationDtoMapper;
import co.com.crediya.solicitudes.api.mapper.PageResponseMapper;
import co.com.crediya.solicitudes.api.mapper.UpdateApplicationStatusMapper;
import co.com.crediya.solicitudes.api.utils.PaginationUtils;
import co.com.crediya.solicitudes.api.validator.RequestValidator;
import co.com.crediya.solicitudes.usecase.application.ApplicationUseCase;
import co.com.crediya.solicitudes.usecase.application.UpdateApplicationStatusUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final UpdateApplicationStatusUseCase updateApplicationStatusUseCase;
    private final ApplicationDtoMapper applicationDtoMapper;
    private final PageResponseMapper pageResponseMapper;
    private final RequestValidator requestValidator;
    private final UpdateApplicationStatusMapper updateApplicationStatusMapper;

    public Mono<ServerResponse> createApplication(ServerRequest request) {
        return request.bodyToMono(CreateApplicationRequest.class)
                .doOnNext(req -> log.info("Received application request for document: {}", req.getDocumentId()))
                .flatMap(req -> requestValidator.validate(req, "createApplicationRequest"))
                .flatMap(validatedReq -> {
                    var application = applicationDtoMapper.toDomain(validatedReq);
                    return applicationUseCase.createApplication(application, validatedReq.getLoanType());
                })
                .flatMap(application -> {
                    ApplicationCreatedResponse response = ApplicationCreatedResponse.success();
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                })
                .doOnSuccess(response -> log.info("Application created successfully"))
                .doOnError(error -> log.error("Error creating application: {}", error.getMessage()));
    }

    public Mono<ServerResponse> getAllApplications(ServerRequest serverRequest) {
        log.info("Getting applications for manual review");

        return PaginationUtils.extractPaginationParams(serverRequest)
                .flatMap(pageRequest -> {
                    log.info("Pagination params - page: {}, size: {}", pageRequest.page(), pageRequest.size());
                    return applicationUseCase.getApplicationsForManualReviewPaginated(pageRequest);
                })
                .flatMap(pageResponseMapper::buildPageResponseWithDetails)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .doOnError(error -> log.error("Error retrieving applications for manual review: {}", error.getMessage()));
    }

    public Mono<ServerResponse> updateApplicationStatus(ServerRequest serverRequest) {
        log.info("Received request to update application status");

        return Mono.fromCallable(() -> updateApplicationStatusMapper.parseApplicationId(serverRequest.pathVariable("id")))
                .flatMap(applicationId ->
                        serverRequest.bodyToMono(UpdateApplicationStatusRequest.class)
                                .flatMap(request -> requestValidator.validate(request, "updateApplicationStatusRequest"))
                                .flatMap(validatedRequest ->
                                        updateApplicationStatusUseCase.updateApplicationStatus(
                                                        applicationId,
                                                        validatedRequest.getStatus(),
                                                        validatedRequest.getComments())
                                                .doOnSuccess(result ->
                                                        log.info("Application status updated manually to: {} for application ID: {} with SQS notification sent", 
                                                                validatedRequest.getStatus(), applicationId))
                                                .map(result -> updateApplicationStatusMapper.toResponse(
                                                        result,
                                                        applicationId,
                                                        validatedRequest.getComments()))
                                )
                )
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .doOnSuccess(response -> log.info("Application status updated successfully with SQS notification"))
                .onErrorResume(error -> {
                    log.error("Error updating application status: {}", error.getMessage());
                    return Mono.error(error);
                });
    }

}
