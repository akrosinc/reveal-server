package com.revealprecision.revealserver.persistence.domain;

import java.util.Set;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.envers.Audited;


@FieldNameConstants
@Audited
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class PlanLocations {
  @Id
  @GeneratedValue
  private UUID identifier;

  @ManyToOne
  @JoinColumn(name = "plan_identifier")
  private Plan plan;

  @ManyToOne
  @JoinColumn(name = "location_identifier")
  private Location location;

  @ManyToMany(mappedBy = "planLocations")
  private Set<Organization> organizations;

  public PlanLocations(Plan plan, Location location) {
    this.plan = plan;
    this.location = location;
  }
}
