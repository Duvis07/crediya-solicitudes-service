package co.com.crediya.solicitudes.api.utils;

import co.com.crediya.solicitudes.model.application.Application;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

@UtilityClass
@Slf4j
public class LoanCalculationUtils {

    private static final int DECIMAL_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Calcula el pago mensual de una solicitud de préstamo
     * @param application La solicitud de préstamo
     * @return El pago mensual calculado
     */
    public static BigDecimal calculateMonthlyPayment(Application application) {
        if (application.getAmount() == null || application.getTerm() == null || application.getTerm() <= 0) {
            log.warn("Invalid application data for monthly payment calculation: amount={}, term={}", 
                    application.getAmount(), application.getTerm());
            return BigDecimal.ZERO;
        }

        BigDecimal monthlyPayment = application.getAmount()
                .divide(BigDecimal.valueOf(application.getTerm()), DECIMAL_SCALE, ROUNDING_MODE);
        
        log.debug("Calculated monthly payment: {} for amount: {} and term: {}", 
                monthlyPayment, application.getAmount(), application.getTerm());
        
        return monthlyPayment;
    }

    public static BigDecimal addMonthlyPayments(BigDecimal payment1, BigDecimal payment2) {
        if (payment1 == null) payment1 = BigDecimal.ZERO;
        if (payment2 == null) payment2 = BigDecimal.ZERO;
        return payment1.add(payment2);
    }
}
