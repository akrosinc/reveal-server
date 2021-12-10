package com.revealprecision.revealserver.persistence.domain;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Getter
@Setter
@RequiredArgsConstructor
@SQLDelete(sql = "UPDATE location_relationship SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
public class LocationRelationship extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;
  private UUID location_hierarchy_identifier;
  private UUID location_identifier;
  private UUID parent_identifier;
  private String ancestry;


}
