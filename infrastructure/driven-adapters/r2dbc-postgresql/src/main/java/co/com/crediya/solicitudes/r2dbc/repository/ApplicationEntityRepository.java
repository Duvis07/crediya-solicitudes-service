package co.com.crediya.solicitudes.r2dbc.repository;

import co.com.crediya.solicitudes.r2dbc.dto.ApplicationWithDetailsDto;
import co.com.crediya.solicitudes.r2dbc.entity.ApplicationEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ApplicationEntityRepository extends ReactiveCrudRepository<ApplicationEntity, Long> {
    
    @Query("""
        SELECT 
            a.application_id,
            a.document_id,
            a.amount,
            a.term,
            a.email,
            a.state_id,
            a.loan_type_id,
            a.created_at,
            a.updated_at,
            s.name as status_name,
            s.description as status_description,
            lt.name as loan_type_name
        FROM applications a
        LEFT JOIN states s ON a.state_id = s.state_id
        LEFT JOIN loan_types lt ON a.loan_type_id = lt.loan_type_id
        ORDER BY a.created_at DESC
        """)
    Flux<ApplicationWithDetailsDto> findAllWithDetails();
}
