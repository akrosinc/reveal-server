package com.revealprecision.revealserver.persistence.domain;

import javax.persistence.CascadeType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Getter
@Setter
@SQLDelete(sql = "UPDATE person_group SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
@Table(name = "person_group")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PersonGroup extends AbstractAuditableEntity {

  @EmbeddedId
  PersonGroupKey personGroupKey;

  @ManyToOne(cascade = CascadeType.ALL)
  @MapsId("groupIdentifier")
  @JoinColumn(name = "group_identifier",referencedColumnName = "identifier")
  Group group;

  @ManyToOne(cascade = CascadeType.ALL)
  @MapsId("personIdentifier")
  @JoinColumn(name = "person_identifier",referencedColumnName = "identifier")
  Person person;
}
