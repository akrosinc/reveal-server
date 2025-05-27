package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.aggregation.ImportAggregationNumeric;
import com.revealprecision.revealserver.persistence.projection.AggregateWithTagProjection;
import com.revealprecision.revealserver.persistence.projection.EntityTagWithGeoLevelProjection;
import com.revealprecision.revealserver.persistence.projection.ImportAggregateNumericProjection;
import com.revealprecision.revealserver.persistence.projection.ImportAggregateStringCountProjection;
import java.util.List;
import java.util.UUID;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportAggregateRepository extends JpaRepository<ImportAggregationNumeric, UUID> {

    @Query(value = "SELECT et as tag, "
            + "SUM(ag.val) as sum, "
            + "AVG(ag.val) as avg, "
            + "AVG(ag.val) as median, "
            + "MIN(ag.val) as min, "
            + "MAX(ag.val) as max, "
            + "ag.ancestor as locationIdentifier, \n"
            + "ag.eventType as eventType \n"
            + "from ImportAggregationNumeric ag \n"
            + "join EntityTag et on ag.fieldCode = et.definition \n"
            + "where ag.ancestor in :locationsIds \n"
            + "and et.identifier in :tagsIds "
            + "group by et.identifier, ag.ancestor, ag.eventType ")
    List<AggregateWithTagProjection> getValuesForTagAndLocations(@Param("tagsIds") List<UUID> tagsIds, @Param("locationsIds") List<String> locationsIds);

    @Query(value = "SELECT ean2.hierarchyidentifier as hierarchyIdentifier, ean2.name as name,\n"
            + "       cast(ean2.locationIdentifier as varchar) as locationIdentifier,\n"
            + "       cast(ean2.planIdentifier as varchar) as planIdentifier,\n"
            + "       ean2.eventType as eventType,\n"
            + "       ean2.fieldcode as fieldCode,\n"
            + "       ean2.sum as sum,\n"
            + "       ean2.avg as avg,\n"
            + "       ean2.median as median,\n"
            + "       ean2.min as min,\n"
            + "       ean2.max as max\n"
            + "from import_aggregate_numeric ean2\n"
            + "\n"
            + "WHERE cast(ean2.locationIdentifier as varchar) in :locationIdentifiers "
            + "and ean2.hierarchyIdentifier = :hierarchyIdentifier", nativeQuery = true)
    List<ImportAggregateNumericProjection> getAggregationValuesByLocationList(
            @Param("locationIdentifiers") List<String> locationIdentifiers, String hierarchyIdentifier);


    @Query(value = "SELECT ean2.hierarchyidentifier as hierarchyIdentifier,  ean2.name as name,\n"
            + "       cast(ean2.locationIdentifier as varchar) as locationIdentifier,\n"
            + "       cast(ean2.planIdentifier as varchar) as planIdentifier,\n"
            + "       ean2.eventType as eventType,\n"
            + "       ean2.fieldCode as fieldCode,\n"
            + "       ean2.fieldVal as fieldVal,\n"
            + "       ean2.count as count\n"
            + "from import_aggregate_string_count ean2\n"
            + "WHERE cast(ean2.locationIdentifier as varchar) in :locationIdentifiers  "
            + "and ean2.hierarchyIdentifier = :hierarchyIdentifier\n", nativeQuery = true)
    List<ImportAggregateStringCountProjection> getAggregationCountValuesByLocationList(
            @Param("locationIdentifiers") List<String> locationIdentifiers, String hierarchyIdentifier);

    @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY import_aggregate_string_count", nativeQuery = true)
    @Transactional
    @Modifying
    void refreshImportAggregateStringCountMaterializedView();

    @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY import_aggregate_numeric", nativeQuery = true)
    @Transactional
    @Modifying
    void refreshImportAggregateNumericMaterializedView();

    @Query(value = "SELECT DISTINCT concat(fieldcode,'-sum') from import_aggregate_numeric ian WHERE  ian.hierarchyIdentifier =:hierarchyIdentifier\n"
            + "UNION ALL \n"
            + "SELECT DISTINCT concat(fieldcode,'-median') from import_aggregate_numeric ian WHERE  ian.hierarchyIdentifier =:hierarchyIdentifier\n"
            + "UNION ALL\n"
            + "SELECT DISTINCT concat(fieldcode,'-average') from import_aggregate_numeric ian WHERE ian.hierarchyIdentifier =:hierarchyIdentifier\n"
            + "UNION ALL\n"
            + "SELECT DISTINCT concat(fieldcode,'-min') from import_aggregate_numeric ian WHERE ian.hierarchyIdentifier =:hierarchyIdentifier\n"
            + "UNION ALL\n"
            + "SELECT DISTINCT concat(fieldcode,'-max') from import_aggregate_numeric ian WHERE ian.hierarchyIdentifier =:hierarchyIdentifier\n"
            + "UNION ALL\n"
            + "SELECT DISTINCT concat(fieldcode,'-count') from import_aggregate_string_count iasc WHERE iasc.hierarchyIdentifier =:hierarchyIdentifier\n", nativeQuery = true)
    List<String> getUniqueDataTagsAssociatedWithData(String hierarchyIdentifier);

    @Query(value = "SELECT concat(t.fieldcode,'-sum')\n"
            + "     ,concat(t.fieldcode,'-median')\n"
            + "     ,concat(t.fieldcode,'-average')\n"
            + "     ,concat(t.fieldcode,'-min')\n"
            + "     ,concat(t.fieldcode,'-max')\n"
            + "     ,concat(t.fieldcode,'-count')\n"
            + "     ,t.hierarchyidentifier, array_agg(t.name) as levels from (\n"
            + "SELECT DISTINCT\n"
            + "                ian.hierarchyidentifier,\n"
            + "                ian.fieldcode,\n"
            + "       gl.name,\n"
            + "                gl.identifier\n"
            + "from import_aggregate_numeric ian\n"
            + "         left join location l on cast(l.identifier as varchar) = ian.locationidentifier\n"
            + "         left join geographic_level gl on gl.identifier = l.geographic_level_identifier\n"
            + "    ) as t\n"
            + "WHERE hierarchyidentifier = :hierarchyIdentifier\n"
            + "group by t.fieldcode, t.hierarchyidentifier", nativeQuery = true)
    List<String> getUniqueDataTagsAndLevelsAssociatedWithData(String hierarchyIdentifier);

    @Query(value = "SELECT DISTINCT\n"
            + "    concat(ian.fieldcode,'-sum') as tagName,\n"
            + "    gl.name as geoName,\n"
            + "    cast(gl.identifier as varchar ) as geoIdentifier\n"
            + "from import_aggregate_numeric ian\n"
            + "         left join location l on cast(l.identifier as varchar) = ian.locationidentifier\n"
            + "         left join geographic_level gl on gl.identifier = l.geographic_level_identifier\n"
            + "WHERE ian.hierarchyidentifier = :hierarchyIdentifier\n"
            + "UNION ALL\n"
            + "SELECT DISTINCT\n"
            + "    concat(ian.fieldcode,'-min') as tagName,\n"
            + "    gl.name as geoName,\n"
            + "    cast(gl.identifier as varchar ) as geoIdentifier\n"
            + "from import_aggregate_numeric ian\n"
            + "         left join location l on cast(l.identifier as varchar) = ian.locationidentifier\n"
            + "         left join geographic_level gl on gl.identifier = l.geographic_level_identifier\n"
            + "WHERE ian.hierarchyidentifier = :hierarchyIdentifier\n"
            + "UNION ALL\n"
            + "SELECT DISTINCT\n"
            + "    concat(ian.fieldcode,'-max') as tagName,\n"
            + "    gl.name as geoName,\n"
            + "    cast(gl.identifier as varchar ) as geoIdentifier\n"
            + "from import_aggregate_numeric ian\n"
            + "         left join location l on cast(l.identifier as varchar) = ian.locationidentifier\n"
            + "         left join geographic_level gl on gl.identifier = l.geographic_level_identifier\n"
            + "WHERE ian.hierarchyidentifier = :hierarchyIdentifier\n"
            + "UNION ALL\n"
            + "SELECT DISTINCT\n"
            + "    concat(ian.fieldcode,'-median') as tagName,\n"
            + "    gl.name as geoName,\n"
            + "    cast(gl.identifier as varchar ) as geoIdentifier\n"
            + "from import_aggregate_numeric ian\n"
            + "         left join location l on cast(l.identifier as varchar) = ian.locationidentifier\n"
            + "         left join geographic_level gl on gl.identifier = l.geographic_level_identifier\n"
            + "WHERE ian.hierarchyidentifier = :hierarchyIdentifier\n"
            + "UNION ALL\n"
            + "SELECT DISTINCT\n"
            + "    concat(ian.fieldcode,'-average') as tagName,\n"
            + "    gl.name as geoName,\n"
            + "    cast(gl.identifier as varchar ) as geoIdentifier\n"
            + "from import_aggregate_numeric ian\n"
            + "         left join location l on cast(l.identifier as varchar) = ian.locationidentifier\n"
            + "         left join geographic_level gl on gl.identifier = l.geographic_level_identifier\n"
            + "WHERE ian.hierarchyidentifier = :hierarchyIdentifier \n"
            + "UNION ALL \n"
            + "SELECT DISTINCT\n"
            + "    concat(ian.fieldcode,'-count') as tagName,\n"
            + "    gl.name as geoName,\n"
            + "    cast(gl.identifier as varchar ) as geoIdentifier\n"
            + "from import_aggregate_string_count ian\n"
            + "         left join location l on cast(l.identifier as varchar) = ian.locationidentifier\n"
            + "         left join geographic_level gl on gl.identifier = l.geographic_level_identifier\n"
            + "WHERE ian.hierarchyidentifier = :hierarchyIdentifier"
            + "", nativeQuery = true)
    List<EntityTagWithGeoLevelProjection> getUniqueDataTagsAndLevelsListAssociatedWithData(String hierarchyIdentifier);

    @Query(value = "WITH distinct_fieldcodes AS (\n" +
            "  SELECT DISTINCT fieldcode\n" +
            "  FROM import_aggregate_numeric \n" +
            "  WHERE hierarchyidentifier = :hierarchyId\n" +
            "\n" +
            "),\n" +
            "suffixes AS (\n" +
            "  SELECT unnest(ARRAY['-sum','-min','-max','-median','-average','-count']) AS suffix\n" +
            ")\n" +
            "SELECT CONCAT(d.fieldcode, s.suffix) AS tagName\n" +
            "FROM distinct_fieldcodes d\n" +
            "CROSS JOIN suffixes s", nativeQuery = true)
    List<String> getUniqueTagsAggregatesForHierarchy(String hierarchyId);

    @Query(value =
        "  SELECT DISTINCT fieldcode\n" +
        "  FROM import_aggregate_numeric \n" +
        "  WHERE hierarchyidentifier = :hierarchyId\n" , nativeQuery = true)
    List<String> getUniqueTagsForHierarchy(String hierarchyId);
}
