package co.com.crediya.solicitudes.api.mapper;

import co.com.crediya.solicitudes.api.dto.UpdateApplicationStatusResponse;
import co.com.crediya.solicitudes.model.application.UpdateApplicationStatusResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UpdateApplicationStatusMapper {

    @Mapping(target = "applicationId", source = "applicationId")
    @Mapping(target = "previousStatus", source = "result.previousStateName")
    @Mapping(target = "newStatus", source = "result.newStateName")
    @Mapping(target = "message", constant = "Status updated successfully")
    @Mapping(target = "updatedAt", source = "result.application.updatedAt")
    @Mapping(target = "comments", source = "comments")
    UpdateApplicationStatusResponse toResponse(
            UpdateApplicationStatusResult result, 
            Long applicationId, 
            String comments);

    default Long parseApplicationId(String pathVariable) {
        if (pathVariable == null || pathVariable.trim().isEmpty()) {
            throw new IllegalArgumentException("Application ID cannot be null or empty");
        }
        try {
            return Long.parseLong(pathVariable.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid application ID format: " + pathVariable);
        }
    }
}
