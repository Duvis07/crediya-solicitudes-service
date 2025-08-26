package co.com.crediya.solicitudes.r2dbc.mapper;

import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.r2dbc.entity.ApplicationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    ApplicationEntity toEntity(Application application);

    Application toDomain(ApplicationEntity entity);
}
