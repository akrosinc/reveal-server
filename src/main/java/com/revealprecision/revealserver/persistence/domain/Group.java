package com.revealprecision.revealserver.persistence.domain;

import java.util.Set;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Getter
@Setter
@SQLDelete(sql = "UPDATE \"group\" SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
@Table(name = "\"group\"")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class Group extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  private String name;

  @ColumnDefault(value = "family")
  private String type;

  @ManyToOne
  @JoinColumn(name = "location_identifier", referencedColumnName = "identifier")
  private Location location;


  @ManyToMany(mappedBy = "groups")
  private Set<Person> persons;


}
