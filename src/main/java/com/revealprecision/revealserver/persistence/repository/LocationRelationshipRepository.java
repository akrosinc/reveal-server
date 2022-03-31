package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRelationshipRepository extends JpaRepository<LocationRelationship, UUID> {

  Optional<LocationRelationship> findByLocationHierarchyIdentifierAndLocationIdentifier(
      UUID locationHierarchyIdentifier, UUID locationIdentifier);

  Optional<List<LocationRelationship>> findByLocationHierarchyIdentifier(
      UUID locationHierarchyIdentifier);

//  @Query(value = "select *"
//      + "    from location_relationship lr "
//      + "    left join location l on l.identifier = lr.location_identifier "
//      + "    left join geographic_level gl on gl.identifier = l.geographic_level_identifier "
//      + "    where lr.location_hierarchy_identifier = :locationHierarchyIdentifier "
//      + "      and gl.name <> 'structure'", nativeQuery = true)
  @Query(value = "select lr, l, pl from LocationRelationship lr "
      + "JOIN lr.location l "
      + "JOIN lr.parentLocation pl "
      + "JOIN l.geographicLevel gl "
      + "WHERE lr.locationHierarchy.identifier = :locationHierarchyIdentifier "
      + "AND gl.name <> 'structure'")
  Optional<List<LocationRelationship>>findByLocationHierarchyWithoutStructures(UUID locationHierarchyIdentifier);

  @Query(value = "SELECT ST_Contains (ST_AsText(ST_GeomFromGeoJSON(:parent)),ST_AsText(ST_Centroid(ST_GeomFromGeoJSON(:child))))", nativeQuery = true)
  Boolean hasParentChildRelationship(@Param("parent") String parent,
      @Param("child") String child);

  @Query(value = "select lr.location.identifier "
      + "from LocationRelationship lr "
      + "where lr.locationHierarchy.identifier = :hierarchyIdentifier "
      + "and lr.location.identifier in :locations")
  List<UUID> findLocationsInHierarchy(
      @Param("hierarchyIdentifier") UUID hierarchyIdentifier,
      @Param("locations") Set<UUID> locations);

  @Query(value = "select lr.identifier "
      + "from LocationRelationship lr "
      + "where lr.parentLocation.identifier= :parentLocationIdentifier")
  List<UUID> findLocationRelationshipUuidsByParentLocation_Identifier(
      UUID parentLocationIdentifier);

  @Query(value = "select "
      + "new com.revealprecision.revealserver.persistence.projection.PlanLocationDetails(lr.location, count(pl), count(pa)) from LocationRelationship lr "
      + "left join PlanLocations pl on lr.location.identifier = pl.location.identifier and pl.plan.identifier = :planIdentifier "
      + "left join PlanAssignment pa on pa.planLocations.identifier = pl.identifier "
      + "where lr.parentLocation.identifier = :parentLocationIdentifier group by lr.identifier")
  List<PlanLocationDetails> getLocationDetailsByParentIdAndPlanId(
      @Param("parentLocationIdentifier") UUID parentLocationIdentifier,
      @Param("planIdentifier") UUID planIdentifier);


  @Query(value = "select lr.location "
      + "from LocationRelationship lr "
      + "where lr.locationHierarchy.identifier = :hierarchyIdentifier "
      + "and lr.parentLocation.identifier = :locationIdentifier")
  List<Location> getChildren(@Param("hierarchyIdentifier") UUID hierarchyIdentifier, @Param("locationIdentifier") UUID locationIdentifier);
}
