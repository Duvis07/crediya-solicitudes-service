package co.com.crediya.solicitudes.api.mapper;

import co.com.crediya.solicitudes.api.dto.ApplicationDetailResponse;
import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.common.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PageResponseMapper {

    private final ApplicationDetailMapper applicationDetailMapper;

    public Mono<Map<String, Object>> buildPageResponseWithDetails(PageResponse<Application> pageResponse) {
        if (pageResponse.content().isEmpty()) {
            return Mono.just(buildPageMetadata(pageResponse, List.of()));
        }
        
        return Flux.fromIterable(pageResponse.content())
                .flatMap(applicationDetailMapper::toDetailResponse)
                .collectList()
                .map(applicationDetails -> buildPageMetadata(pageResponse, applicationDetails))
                .doOnSuccess(response -> log.debug("Built page response with {} items", 
                        ((List<?>) response.get("content")).size()))
                .onErrorResume(error -> {
                    log.error("Error building page response: {}", error.getMessage());
                    return Mono.just(buildPageMetadata(pageResponse, List.of()));
                });
    }
    
    private Map<String, Object> buildPageMetadata(PageResponse<Application> pageResponse, 
                                                  List<ApplicationDetailResponse> content) {
        return Map.of(
                "content", content,
                "page", pageResponse.page(),
                "size", pageResponse.size(),
                "totalElements", pageResponse.totalElements(),
                "first", pageResponse.first(),
                "last", pageResponse.last()
        );
    }
}
