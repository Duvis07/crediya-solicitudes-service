package co.com.crediya.solicitudes.model.common;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty
) {
    public static <T> PageResponse<T> of(List<T> content, PageRequest pageRequest, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / pageRequest.size());

        return new PageResponse<>(
                content,
                pageRequest.page(),
                pageRequest.size(),
                totalElements,
                totalPages,
                pageRequest.page() == 0,
                pageRequest.page() >= totalPages - 1,
                content.isEmpty()
        );
    }
}
