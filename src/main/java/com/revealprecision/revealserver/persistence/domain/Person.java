package com.revealprecision.revealserver.persistence.domain;

import java.util.Date;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Getter
@Setter
@SQLDelete(sql = "UPDATE person SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class Person extends AbstractAuditableEntity {

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

  @ManyToMany
  @JoinTable(name = "person_location",
      joinColumns = @JoinColumn(name = "person_identifier"),
      inverseJoinColumns = @JoinColumn(name = "location_identifier")
  )
  private Set<Location> locations;

  @OneToOne(mappedBy = "person")
  private PersonMetadata personMetadata;

  public Person(UUID identifier, String name) {
    this.identifier = identifier;
    this.nameText = name;
  }

}
