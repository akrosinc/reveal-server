package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import java.util.List;
import java.util.UUID;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanAssignmentRepository extends EntityGraphJpaRepository<PlanAssignment, UUID> {

  List<PlanAssignment> findPlanAssignmentsByOrganization_Identifier(UUID organizationIdentifier);


  @Query(value = "select new com.revealprecision.revealserver.persistence.domain.PlanAssignment(pl.identifier, pl.organization.identifier, pl.organization.name, pl.planLocations.identifier, pl.planLocations.location.identifier) from PlanAssignment pl where pl.planLocations.plan.identifier = :planIdentifier")
  List<PlanAssignment> findPlanAssignmentsByPlanLocations_Plan_Identifier(UUID planIdentifier);

  Integer deletePlanAssignmentsByPlanLocations_Plan_IdentifierAndPlanLocations_Location_Identifier(
      UUID planIdentifier, UUID locationIdentifier);

  @Modifying
  @Transactional
  void deletePlanAssignmentsByPlanLocations_Plan_Identifier(UUID planIdentifier);

  List<PlanAssignment> findPlanAssignmentsByPlanLocations_Plan_IdentifierAndPlanLocations_Location_Identifier(
      UUID planIdentifier, UUID locationIdentifier);

  @Query(value = "select pa from PlanAssignment pa where pa.organization.identifier in :identifiers")
  List<PlanAssignment> findPlansByOrganization(@Param("identifiers") List<UUID> identifiers);
}