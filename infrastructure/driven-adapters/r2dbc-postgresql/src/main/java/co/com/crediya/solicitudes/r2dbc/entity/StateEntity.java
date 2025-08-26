package co.com.crediya.solicitudes.r2dbc.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("states")
public class StateEntity {

    @Id
    @Column("state_id")
    private Long stateId;

    @Column("name")
    private String name;

    @Column("description")
    private String description;
}
