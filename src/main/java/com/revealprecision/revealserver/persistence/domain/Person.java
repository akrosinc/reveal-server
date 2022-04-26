package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.revealprecision.revealserver.persistence.domain.metadata.PersonMetadata;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
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
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class Person extends AbstractAuditableEntity {

  @Id
  @GeneratedValue(generator = "custom-generator")
  @GenericGenerator(name = "custom-generator", strategy = "com.revealprecision.revealserver.persistence.generator.CustomIdentifierGenerator")
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

  private Date deathDate;

  private boolean birthDateApprox;

  private boolean deathDateApprox;

  @ManyToMany
  @JoinTable(name = "person_group", joinColumns = @JoinColumn(name = "person_identifier"), inverseJoinColumns = @JoinColumn(name = "group_identifier"))
  private Set<Group> groups;

  @ManyToMany
  @JoinTable(name = "person_location", joinColumns = @JoinColumn(name = "person_identifier"), inverseJoinColumns = @JoinColumn(name = "location_identifier"))
  private Set<Location> locations; //TODO: Design needs to be updated on this person location story, what about initialization, especially with other parts of code depending on these locations

  @OneToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "task_person", joinColumns = @JoinColumn(name = "person_identifier"), inverseJoinColumns = @JoinColumn(name = "task_identifier"))
  private Set<Task> tasks;

  @OneToOne(mappedBy = "person")
  private PersonMetadata personMetadata;

  public Person(UUID identifier, String name) {
    this.identifier = identifier;
    this.nameText = name;
  }

  @Type(type = "jsonb")
  private JsonNode additionalInfo;

}
