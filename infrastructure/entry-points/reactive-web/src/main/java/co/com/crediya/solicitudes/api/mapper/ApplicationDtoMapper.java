package co.com.crediya.solicitudes.api.mapper;

import co.com.crediya.solicitudes.api.dto.CreateApplicationRequest;
import co.com.crediya.solicitudes.api.dto.ApplicationResponse;
import co.com.crediya.solicitudes.model.application.Application;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApplicationDtoMapper {

    Application toDomain(CreateApplicationRequest request);

    ApplicationResponse toResponse(Application application);

}
