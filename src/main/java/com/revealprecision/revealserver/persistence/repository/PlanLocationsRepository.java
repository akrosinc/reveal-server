package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.PlanLocations;
import com.revealprecision.revealserver.persistence.projection.PlanLocationsAssigned;
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


  @Query(value = "SELECT count(*) "
      + "FROM plan_locations pl "
      + "         INNER JOIN location_relationship lr on pl.location_identifier = lr.location_identifier "
      + "         INNER JOIN location l on l.identifier = lr.location_identifier "
      + "         LEFT JOIN geographic_level gl on l.geographic_level_identifier = gl.identifier "
      + "where lr.location_hierarchy_identifier = :locationHierarchyIdentifier  "
      + "  AND gl.name = :geographicLevelName  "
      + "  AND CAST(STRING_TO_ARRAY(:locationIdentifier, ',') as uuid[]) && lr.ancestry "
      + "  and pl.plan_identifier = :planIdentifier ", nativeQuery = true)
  Long getNumberOfAssignedChildrenByGeoLevelNameWithinLocationAndHierarchyAndPlan(
      @Param("geographicLevelName") String geographicLevelName,
      @Param("locationIdentifier") String locationIdentifier,
      @Param("locationHierarchyIdentifier") UUID locationHierarchyIdentifier,
      @Param("planIdentifier") UUID planIdentifier);



  @Query(value = "  SELECT count(*) "
      + "  FROM plan_locations pl "
      + "  INNER JOIN location_relationship lr on pl.location_identifier = lr.location_identifier "
      + "  inner join location_relationship clr on clr.parent_identifier = lr.location_identifier "
      + "  INNER JOIN location cl on cl.identifier = clr.location_identifier "
      + "  LEFT JOIN geographic_level cgl on cl.geographic_level_identifier = cgl.identifier "
      + "  where lr.location_hierarchy_identifier = :locationHierarchyIdentifier "
      + "  and lr.location_identifier = :locationIdentifier "
      + "  and pl.plan_identifier = :planIdentifier", nativeQuery = true)
  Long getAssignedChildrenOfLocationBelow(
      @Param("locationIdentifier") UUID locationIdentifier,
      @Param("locationHierarchyIdentifier") UUID locationHierarchyIdentifier,
      @Param("planIdentifier") UUID planIdentifier);

  @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY assigned_structure_counts",nativeQuery = true)
  @Transactional
  @Modifying
  void refreshAssignedStructureCountsMaterializedView();

  @Query(value = "select new com.revealprecision.revealserver.persistence.projection.PlanLocationsAssigned(l.identifier, l.name) from PlanLocations pl "
      + "left join Location l on pl.location.identifier = l.identifier "
      + "where pl.plan.identifier = :planIdentifier and lower(l.name) like lower(concat('%', :search, '%'))")
  List<PlanLocationsAssigned> getPlanLocationByPlanIdentifierAndSearch(UUID planIdentifier, String search);

  @Query(value = "select new com.revealprecision.revealserver.persistence.projection.PlanLocationsAssigned(l.identifier, l.name) "
      + "from PlanLocations pl "
      + "left join Location l on pl.location.identifier = l.identifier "
      + "left join PlanAssignment pa on pa.planLocations.identifier = pl.identifier "
      + "where pl.plan.identifier = :planIdentifier and pa.organization.identifier = :organizationIdentifier and pa.selected = true")
  List<PlanLocationsAssigned> getAssignedLocationsToTeam(UUID planIdentifier, UUID organizationIdentifier);
  
  @Query(value = "WITH RECURSIVE ancestors(id, parent_id, lvl) AS ( "
      + "            SELECT lr.location_identifier, lr.parent_identifier,1 AS lvl "
      + "            FROM location_relationship lr "
      + "            WHERE lr.parent_identifier in :locationIdentifiers and lr.location_hierarchy_identifier = :hierarchyIdentifier "
      + "            UNION ALL "
      + "            SELECT parent.location_identifier, parent.parent_identifier, child.lvl + 1 AS lvl "
      + "            FROM location_relationship parent "
      + "              JOIN ancestors child ON parent.parent_identifier = child.id "
      + "              join location loc on parent.location_identifier = loc.identifier "
      + "              join geographic_level gl on gl.identifier = loc.geographic_level_identifier "
      + "              join plan_locations pl on pl.location_identifier = loc.identifier and pl.plan_identifier = :planIdentifier "
      + "          where gl.name <> 'structure' and parent.location_hierarchy_identifier = :hierarchyIdentifier "
      + "           ) "
      + "            select distinct cast(a.id as varchar) from ancestors a", nativeQuery = true)
  List<UUID> getChildrenAssignedLocations(UUID planIdentifier, Set<UUID> locationIdentifiers, UUID hierarchyIdentifier);
  
  @Query(value = "select lr.ancestry from LocationRelationship lr "
      + "    left join PlanLocations pl on pl.location.identifier = lr.location.identifier and pl.plan.identifier = :planIdentifier "
      + "    where lr.location.identifier in :locationIdentifiers "
      + "    and lr.locationHierarchy.identifier = :hierarchyIdentifier")
  List<List<UUID>> getParentsAssignedLocations(Set<UUID> locationIdentifiers, UUID planIdentifier, UUID hierarchyIdentifier);

  @Query(value = "select pl.location.identifier from PlanLocations pl "
      + "join PlanAssignment pa on pa.planLocations.identifier = pl.identifier "
      + "where pl.plan.identifier = :planIdentifier and pa.organization.identifier = :organizationIdentifier ")
  List<UUID> getPlanLocationsIdentifiers(UUID planIdentifier, UUID organizationIdentifier);
}