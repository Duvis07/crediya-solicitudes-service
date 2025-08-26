package co.com.crediya.solicitudes.r2dbc.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("loan_types")
public class LoanTypeEntity {

    @Id
    @Column("loan_type_id")
    private Long id;

    @Column("name")
    private String name;

    @Column("interest_rate")
    private BigDecimal interestRate;

    @Column("minimum_amount")
    private BigDecimal minimumAmount;

    @Column("max_amount")
    private BigDecimal maxAmount;

    @Column("automatic_validation")
    private Boolean automaticValidation;
}
