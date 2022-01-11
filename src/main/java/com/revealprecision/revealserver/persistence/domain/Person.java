package com.revealprecision.revealserver.persistence.domain;

import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
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

//    @ManyToOne
//    @JoinColumn(name = "location_identifier",referencedColumnName = "identifier")
//    private Location location;

    private boolean active;

    @Column(name = "name_use")
    private String nameUse;

    @Column(name = "name_text")
    private String nameText;

    @Column(name = "name_family")
    private String nameFamily;

    @Column(name = "name_given")
    private String nameGiven;

    @Column(name = "name_prefix")
    private String namePrefix;

    @Column(name = "name_suffix")
    private String nameSuffix;


    private String gender;

    private Date birthDate;

}
