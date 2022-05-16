package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.projection.LocationRelationshipProjection;
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

  @Query(value = "SELECT cast(lr.identifier as varchar) identifier, l.name locationName, cast(l.identifier as varchar) locationIdentifier, cast(lr.parent_identifier as varchar) parentIdentifier, gl.name geographicLevelName FROM location_relationship lr "
      + "LEFT JOIN location l ON lr.location_identifier = l.identifier "
      + "LEFT JOIN geographic_level gl ON l.geographic_level_identifier = gl.identifier ", nativeQuery = true)
  List<LocationRelationshipProjection> findByLocationHierarchyIdentifier(
      UUID locationHierarchyIdentifier);

  @Query(value = "SELECT cast(lr.identifier as varchar) identifier, l.name locationName, (select count(*) from location_relationship clr where clr.parent_identifier = lr.location_identifier) as childrenNumber, cast(l.identifier as varchar) locationIdentifier, cast(lr.parent_identifier as varchar) parentIdentifier, gl.name geographicLevelName FROM location_relationship lr "
      + "LEFT JOIN location l ON lr.location_identifier = l.identifier "
      + "LEFT JOIN geographic_level gl ON l.geographic_level_identifier = gl.identifier "
      + "WHERE gl.name NOT LIKE 'structure'", nativeQuery = true)
  List<LocationRelationshipProjection>findByLocationHierarchyWithoutStructures(UUID locationHierarchyIdentifier);

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

  @Query(value = "select lr.location "
      + "from LocationRelationship lr "
      + "where lr.parentLocation.identifier in :parentLocationIdentifier "
      + "and lr.locationHierarchy.identifier = :hierarchyIdentifier")
  List<Location> findLocationRelationshipUuidsByParentLocation_IdentifierAndHierarchyIdentifier(
      @Param("parentLocationIdentifier") List<UUID> parentLocationIdentifiers,
      @Param("hierarchyIdentifier") UUID hierarchyIdentifier);

  @Query(value = "select lr.parentLocation from LocationRelationship lr "
      + "where lr.location.identifier = :locationIdentifier "
      + "and lr.locationHierarchy.identifier = :hierarchyIdentifier")
  Location getParentLocationByLocationIdAndHierarchyId(@Param("locationIdentifier") UUID locationIdentifier, @Param("hierarchyIdentifier") UUID hierarchyIdentifier);
}
