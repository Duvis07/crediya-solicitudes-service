package co.com.crediya.solicitudes.r2dbc.mapper;

import co.com.crediya.solicitudes.model.state.State;
import co.com.crediya.solicitudes.r2dbc.entity.StateEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StateMapper {

    State toDomain(StateEntity entity);
}
