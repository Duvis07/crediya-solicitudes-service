package co.com.crediya.solicitudes.model.common;

public record PageRequest(
        int page,
        int size,
        String sortBy,
        String sortDirection
) {

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size, "createdAt", "DESC");
    }

    public static PageRequest of(int page, int size, String sortBy, String sortDirection) {
        return new PageRequest(page, size, sortBy, sortDirection);
    }

    public long getOffset() {
        return (long) page * size;
    }
}
