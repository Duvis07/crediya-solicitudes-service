package co.com.crediya.solicitudes.model.application;

import co.com.crediya.solicitudes.model.loantype.LoanTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApplicationCommand {
    private String documentId;
    private String email;
    private BigDecimal amount;
    private Integer term;
    private LoanTypeEnum loanType;
}
