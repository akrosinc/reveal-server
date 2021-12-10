package com.revealprecision.revealserver.persistence.domain;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
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
@SQLDelete(sql = "UPDATE location_hierarchy SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
public class LocationHierarchy extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;
  @NotBlank(message = "order is required and must not be empty")
  private String node_order;
}
