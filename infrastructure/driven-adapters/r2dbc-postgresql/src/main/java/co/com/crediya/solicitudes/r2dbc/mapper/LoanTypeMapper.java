package co.com.crediya.solicitudes.r2dbc.mapper;

import co.com.crediya.solicitudes.model.loantype.LoanType;
import co.com.crediya.solicitudes.r2dbc.entity.LoanTypeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoanTypeMapper {

    @Mapping(source = "id", target = "loanTypeId")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "interestRate", target = "interestRate")
    @Mapping(source = "minimumAmount", target = "minimumAmount")
    @Mapping(source = "maxAmount", target = "maxAmount")
    @Mapping(source = "automaticValidation", target = "automaticValidation")
    LoanType toDomain(LoanTypeEntity entity);

    @Mapping(source = "loanTypeId", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "interestRate", target = "interestRate")
    @Mapping(source = "minimumAmount", target = "minimumAmount")
    @Mapping(source = "maxAmount", target = "maxAmount")
    @Mapping(source = "automaticValidation", target = "automaticValidation")
    LoanTypeEntity toEntity(LoanType domain);
}
