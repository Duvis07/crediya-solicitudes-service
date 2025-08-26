package co.com.crediya.solicitudes.r2dbc.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("applications")
public class ApplicationEntity {
    
    @Id
    @Column("application_id")
    private Long applicationId;
    
    @Column("document_id")
    private String documentId;
    
    @Column("amount")
    private BigDecimal amount;
    
    @Column("term")
    private Integer term;
    
    @Column("email")
    private String email;
    
    @Column("state_id")
    private Long stateId;
    
    @Column("loan_type_id")
    private Long loanTypeId;
    
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
