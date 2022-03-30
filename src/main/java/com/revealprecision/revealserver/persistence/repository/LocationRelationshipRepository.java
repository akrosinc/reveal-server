package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
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

  @Query(value = "select *"
      + "    from location_relationship lr "
      + "    join location l on l.identifier = lr.location_identifier "
      + "    join geographic_level gl on gl.identifier = l.geographic_level_identifier "
      + "    where lr.location_hierarchy_identifier = :locationHierarchyIdentifier "
      + "      and gl.name <> 'structure'", nativeQuery = true)
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

  @Query(value = "select lr.location "
      + "from LocationRelationship lr "
      + "where lr.parentLocation.identifier in :parentLocationIdentifier "
      + "and lr.locationHierarchy.identifier = :hierarchyIdentifier")
  List<Location> findLocationRelationshipUuidsByParentLocation_IdentifierAndHierarchyIdentifier(
      @Param("parentLocationIdentifier") List<UUID> parentLocationIdentifiers,
      @Param("hierarchyIdentifier") UUID hierarchyIdentifier);


  @Query(value = "select lr.location "
      + "from LocationRelationship lr "
      + "where lr.locationHierarchy.identifier = :hierarchyIdentifier "
      + "and lr.parentLocation.identifier = :locationIdentifier")
  List<Location> getChildren(@Param("hierarchyIdentifier") UUID hierarchyIdentifier, @Param("locationIdentifier") UUID locationIdentifier);
}
