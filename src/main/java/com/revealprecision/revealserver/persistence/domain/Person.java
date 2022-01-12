package com.revealprecision.revealserver.persistence.domain;

import java.util.List;
import lombok.*;
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

    @OneToMany(cascade = CascadeType.ALL,mappedBy = "person")
    private List<PersonGroup> personGroups;

}
