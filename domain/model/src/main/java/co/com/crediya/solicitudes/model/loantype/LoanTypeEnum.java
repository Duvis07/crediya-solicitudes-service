package co.com.crediya.solicitudes.model.loantype;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
public enum LoanTypeEnum {
    PERSONAL("Prestamo Personal", "PERSONAL"),
    MORTGAGE("Prestamo Hipotecario", "MORTGAGE"),
    VEHICLE("Prestamo Vehicular", "VEHICLE"),
    MICROCREDIT("Microcredito", "MICROCREDIT"),
    BUSINESS("Prestamo Empresarial", "BUSINESS");

    private final String displayName;
    private final String code;
}
