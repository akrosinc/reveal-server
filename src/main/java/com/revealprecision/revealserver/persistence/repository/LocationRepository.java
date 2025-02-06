package com.revealprecision.revealserver.persistence.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.projection.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {

    @Query("SELECT l.identifier from Location l")
    Page<UUID> getAllLocationIdentifiers(Pageable pageable);

    List<Location> findByIdentifierIn(List<UUID> ids);

    @Query("select l.identifier from Location l")
    Set<UUID> findAllIdentifiers();

    @Query("select new com.revealprecision.revealserver.persistence.projection.LocationWithAncestryProjection(l, lr.ancestry, " +
            "count(pa.organization.identifier)) " +
            "from Location l " +
            "left join LocationRelationship lr on lr.location.identifier = l.identifier " +
            "join PlanLocations pl on pl.location.identifier = l.identifier and pl.plan.identifier = :planId " +
            "left join PlanAssignment pa on pa.planLocations.identifier = pl.identifier " +
            "where l.geographicLevel.name in :levelsList  " +
            "group by l.identifier, lr.ancestry ")
    List<LocationWithAncestryProjection> getAllTargetAreasOfPlan(@Param("planId") UUID planId, @Param("levelsList") String levelsList);

    @Query(value = "select cast(l.identifier as varchar) as identifier, l.type as type, to_json(l.geometry as geometry, lr.ancestry as ancestry  \n" +
            "from location l " +
            "left join plan_locations pl on pl.location_identifier = l.identifier " +
            "left join location_relationship lr on lr.location_identifier = l.identifier " +
            "left join geographic_level gl on gl.identifier = l.geographic_level_identifier " +
            "where pl.plan_identifier = :planId " +
            "and gl.name = :levelsList", nativeQuery = true)
    List<LocationWithAncestryProjection> getAllTargetAreasOfPlann(@Param("planId") UUID planId, @Param("levelsList") String levelsList);

    @Query(
            value = "SELECT NOT EXISTS (\n" +
                    "    SELECT 1\n" +
                    "    FROM location l\n" +
                    "    WHERE l.population_data IS NULL\n" +
                    ") ",
            nativeQuery = true)
    boolean populationDataExistsForAll();

    @Query(value = "select new com.revealprecision.revealserver.persistence.projection.LocationWithChildrenCountProjection(l, count(lr), lr2.parentLocation.identifier) " +
            "from Location l " +
            "join GeographicLevel gl on gl.identifier = l.geographicLevel.identifier " +
            "left join LocationRelationship lr on lr.parentLocation.identifier = l.identifier " +
            "left join LocationRelationship lr2 on lr2.location.identifier = l.identifier " +
            "where l.identifier IN :ids " +
            "group by lr.parentLocation.identifier, l.identifier, gl.name, lr2.parentLocation.identifier " +
            "order by " +
            "    CASE gl.name " +
            "        WHEN 'admin0' THEN 1 " +
            "        WHEN 'admin1' THEN 2 " +
            "        WHEN 'admin2' THEN 3 " +
            "        WHEN 'admin3' THEN 4 " +
            "        WHEN 'structure' THEN 5 " +
            "        ELSE 6 " +
            "    END," +
            "   l.identifier")
    Page<LocationWithChildrenCountProjection> findPageableByIdentifierIn(@Param("ids") List<UUID> ids, Pageable pageable);

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

    @Query(value =
            "SELECT  CAST(identifier as varchar) as identifier, name as name, ST_X(ST_Centroid(ST_GeomFromGeoJSON(geometry))) as longitude,ST_Y(ST_Centroid(ST_GeomFromGeoJSON(geometry))) as  latitude from location"
                    + " WHERE identifier in :locationIdentifierList", nativeQuery = true)
    List<LocationCoordinatesProjection> getLocationCentroidCoordinatesByIdentifierList(List<UUID> locationIdentifierList);

    @Query(value = "select ST_AsText(ST_Centroid(st_geomfromgeojson(l.geometry))) from location l where l.identifier = :identifier", nativeQuery = true)
    String getCentroid(@Param("identifier") UUID identifier);

    @Query(value = "select ST_AsText(ST_Centroid(st_geomfromgeojson(l.geometry))) from location l where l.identifier in :identifierList", nativeQuery = true)
    List<String> getCentroidList(@Param("identifierList") List<UUID> identifierList);

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

    @Query(value = "select CAST(lr.location_identifier AS VARCHAR) AS id "
            + "from location_relationship lr "
            + "where lr.parent_identifier = :locationIdentifier and lr.location_hierarchy_identifier = :hierarchyIdentifier ", nativeQuery = true)
    List<UUID> getAllDirectDescendantsOfLocation(UUID locationIdentifier, UUID hierarchyIdentifier);

    @Query(value = "WITH DirectDescendants AS (\n" +
            "    SELECT \n" +
            "        CAST(lr.location_identifier AS VARCHAR) AS id,\n" +
            "        lr.parent_identifier AS parent_id, \n" +
            "        (select array_to_string(array_agg(uuid), ',') from unnest(lr.ancestry) as uuid) \n" +
            "        as ancestry \n" +
            "    FROM \n" +
            "        location_relationship lr\n" +
            "    WHERE \n" +
            "        lr.parent_identifier = :locationIdentifier and lr.location_hierarchy_identifier = :hierarchyIdentifier \n" +
            "    UNION \n" +
            "    SELECT \n" +
            "        CAST(lr.location_identifier AS VARCHAR) AS id, \n" +
            "        COALESCE(lr.parent_identifier, '00000000-0000-0000-0000-000000000000') AS parent_id, \n" +
            "        (select array_to_string(array_agg(uuid), ',') from unnest(lr.ancestry) as uuid) \n" +
            "        as ancestry \n" +
            "    FROM  \n" +
            "        location_relationship lr \n" +
            "    WHERE  \n" +
            "       lr.location_identifier = :locationIdentifier and lr.location_hierarchy_identifier = :hierarchyIdentifier \n" +
            ")\n" +
            "SELECT \n" +
            "    dd.id AS locationId,\n" +
            "    COUNT(lr.location_identifier) AS childrenCount,\n" +
            "    COALESCE(CAST(dd.parent_id AS VARCHAR), '00000000-0000-0000-0000-000000000000') AS parentLocationId,\n" +
            "    CAST(l.population_data AS TEXT) AS populationData,\n" +
            "    dd.id in (\n" +
            "    select cast(pl.location_identifier as VARCHAR) as id\n" +
            "    from plan_locations pl\n" +
            "    where pl.plan_identifier = :planId\n" +
            ") as assigned, \n" +
            "  dd.ancestry as ancestry \n" +
            "FROM \n" +
            "    DirectDescendants dd\n" +
            "LEFT JOIN \n" +
            "    location_relationship lr ON CAST(lr.parent_identifier as VARCHAR) = dd.id\n" +
            "LEFT JOIN \n" +
            "    location l ON CAST(l.identifier as VARCHAR) = dd.id\n" +
            "GROUP BY \n" +
            "    dd.id, dd.parent_id, l.population_data, dd.ancestry ", nativeQuery = true)
    List<LocationDetailsProjection> getAllDirectDescendantsOfLocationWithProperties(@Param("locationIdentifier") UUID locationIdentifier, @Param("hierarchyIdentifier") UUID hierarchyIdentifier, @Param("planId") UUID planId);

    @Query(value = "WITH DirectDescendants AS ( \n" +
            "    SELECT  \n" +
            "        CAST(l.identifier AS VARCHAR) AS id, \n" +
            "        lr.parent_identifier AS parent_id, \n" +
            "\t\tgl.name as level_name, \n" +
            "        (select array_to_string(array_agg(uuid), ',') from unnest(lr.ancestry) as uuid) \n" +
            "        as ancestry \n" +
            "    FROM  \n" +
            "        location l \n" +
            "\tJOIN geographic_level gl on l.geographic_level_identifier = gl.identifier\n" +
            "\tJOIN location_relationship lr on lr.location_identifier = l.identifier \n" +
            "    WHERE  \n" +
            "        gl.name = :geoLevel and lr.location_hierarchy_identifier = :hierarchyIdentifier  \n" +
            ")\n" +
            "SELECT \n" +
            "    dd.id AS locationId,\n" +
            "    COUNT(lr.location_identifier) AS childrenCount,\n" +
            "    COALESCE(CAST(dd.parent_id AS VARCHAR), '00000000-0000-0000-0000-000000000000') AS parentLocationId,\n" +
            "    CAST(l.population_data AS TEXT) AS populationData,\n" +
            "    dd.id in (\n" +
            "    select cast(pl.location_identifier as VARCHAR) as id\n" +
            "    from plan_locations pl\n" +
            "    where pl.plan_identifier = :planId\n" +
            ") as assigned, \n" +
            "  dd.ancestry as ancestry \n" +
            "FROM  \n" +
            "    DirectDescendants dd \n" +
            "LEFT JOIN  \n" +
            "    location_relationship lr ON CAST(lr.parent_identifier as VARCHAR) = dd.id \n" +
            "LEFT JOIN location l ON CAST(l.identifier as VARCHAR) = dd.id\n" +
            "GROUP BY  \n" +
            "    dd.id, dd.parent_id, l.population_data, dd.ancestry", nativeQuery = true)
    List<LocationDetailsProjection> getLocationsWithPropertiesForAdminLevel(@Param("geoLevel") String geoLevel, @Param("hierarchyIdentifier") UUID hierarchyIdentifier, @Param("planId") UUID planId);

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

    @Transactional
    @Modifying
    @Query("UPDATE Location l SET l.populationData = :populationData WHERE l.id = :locationId")
    void updatePopulationData(@Param("locationId") UUID locationId, @Param("populationData") JsonNode populationData);
}
