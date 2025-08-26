package co.com.crediya.solicitudes.model.loantype;

import co.com.crediya.solicitudes.model.exceptions.InvalidApplicationDataException;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;

@Getter
@NoArgsConstructor(force = true)
public enum LoanTypeEnum {
    PERSONAL("Prestamo Personal", "PERSONAL"),
    MORTGAGE("Prestamo Hipotecario", "MORTGAGE"),
    VEHICLE("Prestamo Vehicular", "VEHICLE"),
    MICROCREDIT("Microcredito", "MICROCREDIT"),
    BUSINESS("Prestamo Empresarial", "BUSINESS");

    private final String displayName;
    private final String code;

    LoanTypeEnum(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public static LoanTypeEnum fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new InvalidApplicationDataException("Loan type is required. Valid values: PERSONAL, MORTGAGE, VEHICLE, MICROCREDIT, BUSINESS");
        }

        return Arrays.stream(values())
                .filter(type -> type.code.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseThrow(() -> new InvalidApplicationDataException("Invalid loan type: " + code + ". Valid values: PERSONAL, MORTGAGE, VEHICLE, MICROCREDIT, BUSINESS"));
    }

    public static LoanTypeEnum fromDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new InvalidApplicationDataException("Loan type display name is required");
        }

        return Arrays.stream(values())
                .filter(type -> type.displayName.equalsIgnoreCase(displayName.trim()))
                .findFirst()
                .orElseThrow(() -> new InvalidApplicationDataException("Invalid loan type display name: " + displayName));
    }
}
