package co.com.crediya.solicitudes.r2dbc.mapper;

import co.com.crediya.solicitudes.model.loantype.LoanType;
import co.com.crediya.solicitudes.r2dbc.entity.LoanTypeEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LoanTypeMapper {

    LoanType toDomain(LoanTypeEntity entity);
}
