package com.revealprecision.revealserver.persistence.domain;

import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Audited
@Getter
@Setter
//@RequiredArgsConstructor
@SQLDelete(sql = "UPDATE \"group\" SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
@Table(name = "\"group\"")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Group extends AbstractAuditableEntity{

    @Id
    @GeneratedValue
    private UUID identifier;

    private String name;

    @ColumnDefault(value = "family")
    private String type;

    @ManyToOne
    @JoinColumn(name = "location_identifier",referencedColumnName = "identifier")
    private Location location;

}
