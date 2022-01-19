package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.enums.GenderEnum;
import java.util.List;
import java.util.Set;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Audited
@Getter
@Setter
@SQLDelete(sql = "UPDATE person SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
@Table(name = "person")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class Person extends AbstractAuditableEntity{

    @Id
    @GeneratedValue
    private UUID identifier;

    private boolean active;

    private String nameUse;

    private String nameText;

    private String nameFamily;

    private String nameGiven;

    private String namePrefix;

    private String nameSuffix;

    private String gender;

    private Date birthDate;

    @ManyToMany
    @JoinTable(name = "person_group",
        joinColumns = @JoinColumn(name = "person_identifier"),
        inverseJoinColumns = @JoinColumn(name = "group_identifier")
    )
    private Set<Group> groups;

}
