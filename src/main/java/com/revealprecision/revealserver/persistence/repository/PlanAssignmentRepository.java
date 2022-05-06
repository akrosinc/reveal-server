package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.persistence.projection.PlanLocationProjection;
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
  @Query(value = "delete from plan_assignment pa "
      + "using plan_locations pl "
      + "where pa.plan_locations_identifier = pl.identifier "
      + "and pl.plan_identifier = :planIdentifier", nativeQuery = true)
  void deletePlanAssignmentsByPlanLocations_Plan_Identifier(@Param("planIdentifier") UUID planIdentifier);

  List<PlanAssignment> findPlanAssignmentsByPlanLocations_Plan_IdentifierAndPlanLocations_Location_Identifier(
      UUID planIdentifier, UUID locationIdentifier);

  @Query(value = "select pa from PlanAssignment pa where pa.organization.identifier in :identifiers")
  List<PlanAssignment> findPlansByOrganization(@Param("identifiers") List<UUID> identifiers);

  @Query(value = "select pl.identifier as identifier,pl.location.identifier as locationIdentifier from PlanLocations pl where pl.plan.identifier = :planIdentifier")
  List<PlanLocationProjection> getPlanLocationsIdentifiers(UUID planIdentifier);

  @Modifying
  @Query(value = "delete from plan_assignment pa "
      + "using plan_locations pl "
      + "where pa.plan_locations_identifier = pl.identifier "
      + "and pl.location_identifier in :locationIdentifier "
      + "and pl.plan_identifier = :planIdentifier", nativeQuery = true)
  void deleteAllByPlanIdentifierAndLocationIdentifiers(List<UUID> locationIdentifier, UUID planIdentifier);
}