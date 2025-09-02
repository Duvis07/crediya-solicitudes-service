package co.com.crediya.solicitudes.api.utils;

import co.com.crediya.solicitudes.model.common.PageRequest;
import lombok.experimental.UtilityClass;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

@UtilityClass
public class PaginationUtils {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    public static Mono<PageRequest> extractPaginationParams(ServerRequest serverRequest) {
        try {
            int page = serverRequest.queryParam("page")
                    .map(Integer::parseInt)
                    .orElse(DEFAULT_PAGE);
            int size = serverRequest.queryParam("size")
                    .map(Integer::parseInt)
                    .orElse(DEFAULT_SIZE);

            // Validate pagination parameters
            if (page < 0) {
                return Mono.error(new IllegalArgumentException("Page number cannot be negative"));
            }
            if (size <= 0 || size > MAX_SIZE) {
                return Mono.error(new IllegalArgumentException("Page size must be between 1 and " + MAX_SIZE));
            }

            return Mono.just(PageRequest.of(page, size));
        } catch (NumberFormatException e) {
            return Mono.error(new IllegalArgumentException("Invalid pagination parameters: " + e.getMessage()));
        }
    }
}
