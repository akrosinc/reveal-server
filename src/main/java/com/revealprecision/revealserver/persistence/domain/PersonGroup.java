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
import org.hibernate.envers.Audited;

@Entity
@Getter
@Setter
@Table(name = "person_group")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Audited
public class PersonGroup {

  @EmbeddedId
  PersonGroupKey personGroupKey;

  @ManyToOne(cascade = CascadeType.ALL)
  @MapsId("groupIdentifier")
  @JoinColumn(name = "group_identifier", referencedColumnName = "identifier")
  Group group;

  @ManyToOne(cascade = CascadeType.ALL)
  @MapsId("personIdentifier")
  @JoinColumn(name = "person_identifier", referencedColumnName = "identifier")
  Person person;
}
