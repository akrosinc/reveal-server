package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.util.UserUtils;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
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
@Where(clause = "entity_status='ACTIVE'")
public class PlanAssignment extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  @ManyToOne
  @JoinColumn(name = "organization_identifier", referencedColumnName = "identifier")
  private Organization organization;

  @ManyToOne
  @JoinColumn(name = "plan_locations_identifier", referencedColumnName = "identifier")
  private PlanLocations planLocations;

  public PlanAssignment(Organization organization, PlanLocations planLocations) {
    this.organization = organization;
    this.planLocations = planLocations;
    this.setEntityStatus(EntityStatus.ACTIVE);
    this.setCreatedBy(UserUtils.getKeyCloakPrincipal().getName());
    this.setCreatedDatetime(LocalDateTime.now());
    this.setModifiedBy(UserUtils.getKeyCloakPrincipal().getName());
    this.setModifiedDatetime(LocalDateTime.now());
  }

  public PlanAssignment(UUID identifier, UUID orgIdentifier, String orgName, UUID planLocationIdentifier, UUID locationIdentifier) {
    Organization organization = Organization.builder()
        .identifier(orgIdentifier)
        .name(orgName)
        .build();
    PlanLocations planLocations = PlanLocations.builder()
        .identifier(planLocationIdentifier)
        .location(Location.builder().identifier(locationIdentifier).build())
        .build();
    this.planLocations = planLocations;
    this.organization = organization;
    this.identifier = identifier;
  }

}