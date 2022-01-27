package com.revealprecision.revealserver.persistence.domain;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
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
@AllArgsConstructor
@NoArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE lookup_plan_status SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
public class LookupPlanStatus extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String description;

//  @OneToMany(mappedBy = "status")
//  private Set<Plan> plans;
}
