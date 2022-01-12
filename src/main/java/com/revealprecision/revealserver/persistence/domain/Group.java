package com.revealprecision.revealserver.persistence.domain;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldNameConstants;
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
@SQLDelete(sql = "UPDATE \"group\" SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
@Table(name = "\"group\"",schema = "public")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
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

    @OneToMany(cascade = CascadeType.ALL,mappedBy = "group")
    private List<PersonGroup> personGroups;
}
