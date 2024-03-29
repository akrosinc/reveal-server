package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.projection.LocationCoordinatesProjection;
import com.revealprecision.revealserver.persistence.projection.LocationWithParentProjection;
import com.revealprecision.revealserver.persistence.projection.PlanLocationDetails;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {

  @Query(value = "select l from Location l where l.geographicLevel.identifier = :identifier")
  List<Location> findByGeographicLevelIdentifier(@Param("identifier") UUID identifier);

  @Query(value = "SELECT l FROM Location l WHERE (lower(l.name) like lower(concat('%', :param, '%'))) AND l.entityStatus='ACTIVE'")
  Page<Location> findAlLByCriteria(@Param("param") String param, Pageable pageable);

  @Query(value = "SELECT COUNT(l) FROM Location l WHERE (lower(l.name) like lower(concat('%', :param, '%'))) AND l.entityStatus='ACTIVE'")
  long findAllCountByCriteria(@Param("param") String param);

  @Query(value = "select "
      + " cast(l.identifier as varchar) as identifier,\n"
      + "l.name as name,\n"
      + "l.geometry->>'type' as type,\n"
      + "CAST (l.geometry as varchar) as geometry,\n"
      + "l.status as status,\n"
      + " cast(l.external_id as varchar) as externalId,\n"
      + "CAST(l.location_property as varchar) as locationProperty,\n"
      + "gl.name as geographicLevelName,\n"
      + "l.entity_status as entityStatus,\n"
      + "l.server_version as serverVersion,\n"
      + "l.hash_value as hashValue,\n"
      + "cast(lr.parent_identifier as varchar) as parentIdentifier \n"
      + " from location  l left join geographic_level gl on gl.identifier = l.geographic_level_identifier "
      + "left join location_relationship lr on lr.location_identifier = l.identifier "
      + "where  l.identifier in :identifiers and l.server_version >= :serverVersion and gl.name != 'structure'", nativeQuery = true)
  List<LocationWithParentProjection> getAllNotStructuresByIdentifiersAndServerVersion(
      @Param("identifiers") List<UUID> identifiers, @Param("serverVersion") long serverVersion);

  @Query(value = "select "
      + " cast(l.identifier as varchar) as identifier,\n"
      + "l.name as name,\n"
      + "l.geometry->>'type' as type,\n"
      + "CAST (l.geometry as varchar) as geometry,\n"
      + "l.status as status,\n"
      + " cast(l.external_id as varchar) as externalId,\n"
      + "CAST(l.location_property as varchar) as locationProperty,\n"
      + "gl.name as geographicLevelName,\n"
      + "l.entity_status as entityStatus,\n"
      + "l.server_version as serverVersion,\n"
      + "l.hash_value as hashValue,\n"
      + "cast(lr.parent_identifier as varchar)  as parentIdentifier \n"
      + " from location  l left join geographic_level gl on gl.identifier = l.geographic_level_identifier "
      + "left join location_relationship lr on lr.location_identifier = l.identifier "
      + "where  l.name in :names and l.server_version >= :serverVersion and gl.name != 'structure'", nativeQuery = true)
  List<LocationWithParentProjection> getAllNotStructureByNamesAndServerVersion(@Param("names") List<String> names,
      @Param("serverVersion") long serverVersion);

  @Query(value = "select l from Location  l where  l.name in :names")
  List<Location> getAllByNames(@Param("names") List<String> names);

  @Query(value =
      "SELECT  name as name, ST_X(ST_Centroid(ST_GeomFromGeoJSON(geometry))) as longitude,ST_Y(ST_Centroid(ST_GeomFromGeoJSON(geometry))) as  latitude from location"
          + " WHERE identifier = :locationIdentifier", nativeQuery = true)
  LocationCoordinatesProjection getLocationCentroidCoordinatesByIdentifier(UUID locationIdentifier);

  @Query(value = "select ST_AsText(ST_Centroid(st_geomfromgeojson(l.geometry))) from location l where l.identifier = :identifier", nativeQuery = true)
  String getCentroid(@Param("identifier") UUID identifier);

  List<Location> findLocationsByIdentifierIn(List<UUID> uuids);

  @Query(value =
      "select new com.revealprecision.revealserver.persistence.projection.PlanLocationDetails(l, count(pl), count(pa)) from Location l "
          + "left join PlanLocations pl on l.identifier = pl.location.identifier and pl.plan.identifier = :planIdentifier "
          + "left join PlanAssignment pa on pa.planLocations.identifier = pl.identifier "
          + "left join Plan pn on pn.identifier = :planIdentifier "
          + "left join LocationRelationship lr on lr.locationHierarchy = pn.locationHierarchy.identifier and lr.location.identifier = :locationIdentifier "
          + "where l.identifier = :locationIdentifier group by l.identifier")
  PlanLocationDetails getLocationDetailsByIdentifierAndPlanIdentifier(
      @Param("locationIdentifier") UUID locationIdentifier,
      @Param("planIdentifier") UUID planIdentifier);

  @Query(value = "WITH RECURSIVE ancestors(id, parent_id, lvl) AS ( "
      + "      SELECT lr.location_identifier, lr.parent_identifier,1 AS lvl "
      + "      FROM location_relationship lr "
      + "      WHERE lr.parent_identifier = :locationIdentifier and lr.location_hierarchy_identifier = :hierarchyIdentifier"
      + "      UNION ALL "
      + "      SELECT parent.location_identifier, parent.parent_identifier, child.lvl + 1 AS lvl "
      + "      FROM location_relationship parent "
      + "        JOIN ancestors child ON parent.parent_identifier = child.id "
      + "        join location loc on parent.location_identifier = loc.identifier "
      + "        join geographic_level gl on gl.identifier = loc.geographic_level_identifier "
      + "    where gl.name <> 'structure' and parent.location_hierarchy_identifier = :hierarchyIdentifier"
      + "     ) "
      + "      select cast(a.id as varchar) from ancestors a", nativeQuery = true)
  List<UUID> getAllLocationChildren(UUID locationIdentifier, UUID hierarchyIdentifier);

  @Query(value = "WITH RECURSIVE ancestors(id, parent_id, lvl) AS ( "
      + "      SELECT lr.location_identifier, lr.parent_identifier,1 AS lvl "
      + "      FROM location_relationship lr "
      + "      WHERE lr.parent_identifier = :locationIdentifier and lr.location_hierarchy_identifier = :hierarchyIdentifier"
      + "      UNION ALL "
      + "      SELECT parent.location_identifier, parent.parent_identifier, child.lvl + 1 AS lvl "
      + "      FROM location_relationship parent "
      + "        JOIN ancestors child ON parent.parent_identifier = child.id "
      + "        join location loc on parent.location_identifier = loc.identifier "
      + "        join geographic_level gl on gl.identifier = loc.geographic_level_identifier "
      + "    where gl.name NOT IN :nodeList and parent.location_hierarchy_identifier = :hierarchyIdentifier"
      + "     ) "
      + "      select cast(a.id as varchar) from ancestors a", nativeQuery = true)
  List<UUID> getAllLocationChildrenNotLike(UUID locationIdentifier, UUID hierarchyIdentifier,
      List<String> nodeList);

  List<Location> getLocationsByPeople_Identifier(UUID personIdentifier);

  @Query(value =
      "select new com.revealprecision.revealserver.persistence.domain.Location"
          + "(l.identifier, l.type, l.name, l.status, l.externalId, l.geographicLevel, l.locationBulk)"
          + " from Location l where l.identifier = :identifier")
  Optional<Location> findByIdentifierWithoutGeoJson(UUID identifier);

  @Query(value =
      "select new com.revealprecision.revealserver.persistence.domain.Location"
          + "(l.identifier, l.type, l.name, l.status, l.externalId, l.geographicLevel, l.locationBulk)"
          + " from Location l where l.identifier in :identifiers")
  Set<Location> findLocationsWithoutGeoJsonByIdentifierIn(Set<UUID> identifiers);

  @Query(value = "select l.name from Location l where l.hashValue in :hashes")
  List<String> findAllByHashes(Set<String> hashes);
}
