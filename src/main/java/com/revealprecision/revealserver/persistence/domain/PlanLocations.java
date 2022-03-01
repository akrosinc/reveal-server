package com.revealprecision.revealserver.persistence.domain;

import java.util.Set;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
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
@SQLDelete(sql = "UPDATE \"plan_locations\" SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
@Builder
@Table(name = "plan_locations")
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class PlanLocations {

  @Id
  @GeneratedValue
  private UUID identifier;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "plan_identifier",referencedColumnName = "identifier")
  Plan plan;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "location_identifier", referencedColumnName = "identifier")
  Location location;

  @OneToMany(mappedBy = "planLocations")
  Set<PlanAssignment> planAssignments;


}
