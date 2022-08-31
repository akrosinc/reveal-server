package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.PlanLocations;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanLocationsRepository extends EntityGraphJpaRepository<PlanLocations, UUID> {

  List<PlanLocations> findByPlan_Identifier(UUID planIdentifier);

  List<PlanLocations> findByLocation_IdentifierAndPlan_Identifier(UUID locationIdentifier,
      UUID planIdentifier);

  List<PlanLocations> findByLocation_IdentifierIn(List<UUID> locationIdentifiers);

  PlanLocations findByPlan_IdentifierAndLocation_Identifier(UUID planIdentifier,
      UUID locationIdentifier);

  List<PlanLocations> findByPlan_IdentifierAndLocation_IdentifierIn(UUID planIdentifier,
      List<UUID> locationIdentifier);

  Long countByPlan_Identifier(UUID planIdentifier);

  @Query(value = (
      "select new com.revealprecision.revealserver.persistence.domain.Location(l.identifier, l.type, l.name, l.status, l.externalId, l.geographicLevel, l.locationBulk)"
          + " from PlanLocations pl left join Location l on pl.location.identifier = l.identifier"
          + " where pl.plan.identifier = :planIdentifier"))
  Set<Location> findLocationsByPlan_Identifier(UUID planIdentifier);

  @Transactional
  @Modifying
  @Query(value = "delete from PlanLocations pl where pl.plan.identifier = :planIdentifier and pl.location.identifier in :locations")
  int deletePlanLocationsByPlanAndLocation(@Param("planIdentifier") UUID identifier,
      @Param("locations") List<UUID> locations);


  @Query(value = "select new com.revealprecision.revealserver.persistence.domain.PlanLocations(pl.identifier, pl.location.identifier, pl.plan.identifier) from PlanLocations pl where pl.plan.identifier = :planIdentifier and pl.location.identifier in :locationIdentifiers")
  Set<PlanLocations> getPlanLocationsByPlanIdAndLocationIdentifiers(@Param("planIdentifier")UUID planIdentifier, @Param("locationIdentifiers")List<UUID> locationIdentifiers);

  @Transactional
  @Modifying
  @Query(value = "delete from PlanLocations pl where pl.plan.identifier = :planIdentifier")
  int deleteByPlanIdentifier(UUID planIdentifier);


  @Query(value = "SELECT count(*)\n"
      + "FROM plan_locations pl\n"
      + "         INNER JOIN location_relationship lr on pl.location_identifier = lr.location_identifier\n"
      + "         INNER JOIN location l on l.identifier = lr.location_identifier\n"
      + "         LEFT JOIN geographic_level gl on l.geographic_level_identifier = gl.identifier\n"
      + "where lr.location_hierarchy_identifier = :locationHierarchyIdentifier \n"
      + "  AND gl.name = :geographicLevelName \n"
      + "  AND CAST(STRING_TO_ARRAY(:locationIdentifier, ',') as uuid[]) && lr.ancestry\n"
      + "  and pl.plan_identifier = :planIdentifier ", nativeQuery = true)
  Long getNumberOfAssignedChildrenByGeoLevelNameWithinLocationAndHierarchyAndPlan(
      @Param("geographicLevelName") String geographicLevelName,
      @Param("locationIdentifier") String locationIdentifier,
      @Param("locationHierarchyIdentifier") UUID locationHierarchyIdentifier,
      @Param("planIdentifier") UUID planIdentifier);



  @Query(value = "  SELECT count(*)\n"
      + "  FROM plan_locations pl\n"
      + "  INNER JOIN location_relationship lr on pl.location_identifier = lr.location_identifier\n"
      + "  inner join location_relationship clr on clr.parent_identifier = lr.location_identifier\n"
      + "  INNER JOIN location cl on cl.identifier = clr.location_identifier\n"
      + "  LEFT JOIN geographic_level cgl on cl.geographic_level_identifier = cgl.identifier\n"
      + "  where lr.location_hierarchy_identifier = :locationHierarchyIdentifier\n"
      + "  and lr.location_identifier = :locationIdentifier\n"
      + "  and pl.plan_identifier = :planIdentifier", nativeQuery = true)
  Long getAssignedChildrenOfLocationBelow(
      @Param("locationIdentifier") UUID locationIdentifier,
      @Param("locationHierarchyIdentifier") UUID locationHierarchyIdentifier,
      @Param("planIdentifier") UUID planIdentifier);

  @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY assigned_structure_counts",nativeQuery = true)
  @Transactional
  @Modifying
  void refreshAssignedStructureCountsMaterializedView();
}