package com.revealprecision.revealserver.persistence.domain;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
@SQLDelete(sql = "UPDATE plan SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
public class PlanAssignment extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;


  @OneToOne
  @JoinColumn(name = "organization_identifier",referencedColumnName = "identifier")
  private Organization organization;

  @ManyToOne
  @JoinColumn(name = "plan_locations_identifier",referencedColumnName = "identifier")
  private PlanLocations planLocations;

}