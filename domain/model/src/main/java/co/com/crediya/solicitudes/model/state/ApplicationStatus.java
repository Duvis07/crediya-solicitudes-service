package co.com.crediya.solicitudes.model.state;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
public enum ApplicationStatus {

    PENDING_REVIEW("Pendiente de revision"),
    APPROVED("Aprobada"),
    REJECTED("Rechazada"),
    MANUAL_REVIEW("Revision manual"),
    IN_PROCESS("En proceso"),
    DISBURSED("Desembolsada"),
    COMPLETED("Completada"),
    CANCELLED("Cancelada");

    private final String description;
}
