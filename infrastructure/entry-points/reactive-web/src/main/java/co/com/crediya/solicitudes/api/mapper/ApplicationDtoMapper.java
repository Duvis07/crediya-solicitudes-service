package co.com.crediya.solicitudes.api.mapper;

import co.com.crediya.solicitudes.api.dto.CreateApplicationRequest;
import co.com.crediya.solicitudes.api.dto.ApplicationResponse;
import co.com.crediya.solicitudes.model.application.Application;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApplicationDtoMapper {
    
    ApplicationDtoMapper INSTANCE = Mappers.getMapper(ApplicationDtoMapper.class);
    
    @Mapping(target = "applicationId", ignore = true)
    @Mapping(target = "loanTypeId", ignore = true)
    @Mapping(target = "stateId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Application toDomain(CreateApplicationRequest request);
    
    @Mapping(target = "statusName", ignore = true)
    @Mapping(target = "loanTypeName", ignore = true)
    @Mapping(target = "statusDescription", ignore = true)
    ApplicationResponse toResponse(Application application);
}
