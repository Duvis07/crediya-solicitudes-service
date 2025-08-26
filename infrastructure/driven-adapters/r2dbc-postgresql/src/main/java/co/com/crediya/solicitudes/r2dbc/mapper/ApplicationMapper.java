package co.com.crediya.solicitudes.r2dbc.mapper;

import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.r2dbc.entity.ApplicationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApplicationMapper {
    
    ApplicationMapper INSTANCE = Mappers.getMapper(ApplicationMapper.class);
    
    ApplicationEntity toEntity(Application application);
    
    Application toDomain(ApplicationEntity entity);
}
