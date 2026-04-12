package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.aggregation.ImportAggregationNumeric;
import com.revealprecision.revealserver.persistence.projection.AggregateWithTagProjection;
import com.revealprecision.revealserver.persistence.projection.LocationWithMetadataProjection;
import com.revealprecision.revealserver.persistence.projection.TagYearAggregateDateProjection;
import com.revealprecision.revealserver.persistence.projection.TagYearRangeAggregateDateProjection;
import java.util.List;
import java.util.UUID;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportAggregateByDateRepository extends JpaRepository<ImportAggregationNumeric, UUID> {



    @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY mw_import_aggregate_numeric_by_date", nativeQuery = true)
    @Transactional
    @Modifying
    void refreshMW_import_aggregate_numeric_by_date();


    @Query(value =
        "SELECT " +
            "    CAST(l.identifier AS VARCHAR) AS id, " +
            "    l.name AS name, " +
            "    l.type AS type, " +
            "    CAST(l.geometry AS text) AS geometry, " +
            "    gl.name AS geographicLevel, " +
            "    ian.fieldcode AS tag, " +
            "    ian.sum AS sum, " +
            "    ian.avg AS avg, " +
            "    ian.median AS median, " +
            "    ian.min AS min, " +
            "    ian.max AS max, " +
            "    ian.count AS count, " +
            "    ian.datacapturedate AS dataCaptureDate, " +
            "    ian.year AS year " +
            "FROM location l " +
            "INNER JOIN geographic_level gl ON gl.identifier = l.geographic_level_identifier " +
            "INNER JOIN mw_import_aggregate_numeric_by_date ian " +
            "    ON ian.locationidentifier = CAST(l.identifier AS VARCHAR) " +
            "INNER JOIN entity_tag et on ian.fieldcode = et.definition " +
            "WHERE CAST(l.identifier AS VARCHAR) IN :locationIds " +
            "AND ian.hierarchyidentifier = :hierarchyId " +
            "AND CAST(et.identifier AS VARCHAR) IN (:tagIds ) " +
            "AND (ian.year = :year) " +
            "AND l.entity_status = 'ACTIVE'",
        nativeQuery = true)
    List<LocationWithMetadataProjection> findLocationsWithGeometryAndMetadata(
        @Param("locationIds") List<String> locationIds,
        @Param("hierarchyId") String hierarchyId,
        @Param("tagIds") List<String> tagIds,
        @Param("year") Integer year
    );


    @Query(value =
        "SELECT ian.fieldcode AS tag, MAX(ian.year) AS year,CAST(et.identifier AS VARCHAR)  as tagIdentifier " +
            "FROM mw_import_aggregate_numeric_by_date ian " +
            "INNER JOIN entity_tag et ON ian.fieldcode = et.definition " +
            "WHERE ian.hierarchyidentifier = :hierarchyId " +
            "AND CAST(et.identifier AS VARCHAR) IN (:tagIds) " +
            "GROUP BY et.identifier,ian.fieldcode",
        nativeQuery = true)
    List<TagYearAggregateDateProjection> findLatestYearPerTag(
        @Param("hierarchyId") String hierarchyId,
        @Param("tagIds") List<String> tagIds
    );

    @Query(value =
        "SELECT " +
            "    CAST(et.identifier AS VARCHAR) AS tagIdentifier,"
            + " et.definition as tag, " +
            "    MAX(ian.year) AS maxYear, " +
            "    MIN(ian.year) AS minYear " +
            "FROM mw_import_aggregate_numeric_by_date ian " +
            "INNER JOIN entity_tag et ON ian.fieldcode = et.definition " +
            "WHERE ian.hierarchyidentifier = :hierarchyId " +
            "AND CAST(et.identifier AS VARCHAR) IN (:tagIds) " +
            "GROUP BY et.identifier",
        nativeQuery = true)
    List<TagYearRangeAggregateDateProjection> findYearRangePerTag(String hierarchyId, List<String> tagIds);
}
