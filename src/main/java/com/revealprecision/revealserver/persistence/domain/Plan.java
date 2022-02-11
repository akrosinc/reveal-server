package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.api.v1.dto.request.PlanRequest;
import com.revealprecision.revealserver.enums.PlanStatusEnum;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;
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
public class Plan extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;
  private String name;
  private String title;
  private LocalDate date;
  private LocalDate effectivePeriodStart;
  private LocalDate effectivePeriodEnd;

  @ManyToOne
  @JoinColumn(name = "hierarchy_identifier")
  private LocationHierarchy locationHierarchy;

  @ManyToMany(cascade = CascadeType.PERSIST)
  @JoinTable(
      name = "plan_locations",
      joinColumns = @JoinColumn(name = "plan_identifier"),
      inverseJoinColumns = @JoinColumn(name = "location_identifier"))
  private Set<Location> locations;

  @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL)
  private Set<Goal> goals;

  @NotNull
  @Enumerated(EnumType.STRING)
  private PlanStatusEnum status;

  @ManyToOne
  @JoinColumn(name = "lookup_intervention_type_identifier")
  private LookupInterventionType interventionType;

  public Plan update(PlanRequest request, LocationHierarchy hierarchy,
      LookupInterventionType interventionType) {
    this.title = request.getTitle();
    this.name = request.getName();
    this.date = LocalDate.now();
    this.effectivePeriodStart = request.getEffectivePeriod().getStart();
    this.effectivePeriodEnd = request.getEffectivePeriod().getEnd();
    this.locationHierarchy = hierarchy;
    this.interventionType = interventionType;
    return this;
  }
}