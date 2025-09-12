package co.com.crediya.solicitudes.model.events.gateways;

import co.com.crediya.solicitudes.model.events.LoanApprovedEvent;
import reactor.core.publisher.Mono;

public interface ReportEventRepository {
    
    Mono<Void> sendLoanApprovedEvent(LoanApprovedEvent event);
}
