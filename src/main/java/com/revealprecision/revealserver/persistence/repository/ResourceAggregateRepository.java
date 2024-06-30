package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.aggregation.ResourceAggregationNumeric;
import com.revealprecision.revealserver.persistence.projection.EntityTagProjection;
import com.revealprecision.revealserver.persistence.projection.ResourceAggregateNumericProjection;
import com.revealprecision.revealserver.persistence.projection.EntityTagWithGeoLevelAndEntityTypeProjection;
import java.util.List;
import java.util.UUID;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceAggregateRepository extends
    JpaRepository<ResourceAggregationNumeric, UUID> {

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
      + "from resource_aggregate_numeric ean2\n"
      + "\n"
      + "WHERE cast(ean2.locationIdentifier as varchar) in :locationIdentifiers "
      + "and ean2.hierarchyIdentifier = :hierarchyIdentifier", nativeQuery = true)
  List<ResourceAggregateNumericProjection> getAggregationValuesByLocationList(
      @Param("locationIdentifiers") List<String> locationIdentifiers, String hierarchyIdentifier);


  @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY resource_aggregate_numeric", nativeQuery = true)
  @Transactional
  @Modifying
  void refreshResourceAggregateNumericMaterializedView();

  @Query(value =
      "SELECT DISTINCT ian.eventType ,concat(ian.eventType,'-',ian.fieldcode,'-sum') as tag from resource_aggregate_numeric ian WHERE  ian.hierarchyIdentifier =:hierarchyIdentifier\n"
          + "UNION ALL \n"
          + "SELECT DISTINCT ian.eventType, concat(ian.eventType,'-',ian.fieldcode,'-median') as tag from resource_aggregate_numeric ian WHERE  ian.hierarchyIdentifier =:hierarchyIdentifier\n"
          + "UNION ALL\n"
          + "SELECT DISTINCT ian.eventType, concat(ian.eventType,'-',ian.fieldcode,'-average') as tag from resource_aggregate_numeric ian WHERE ian.hierarchyIdentifier =:hierarchyIdentifier\n"
          + "UNION ALL\n"
          + "SELECT DISTINCT ian.eventType, concat(ian.eventType,'-',ian.fieldcode,'-min') as tag from resource_aggregate_numeric ian WHERE ian.hierarchyIdentifier =:hierarchyIdentifier\n"
          + "UNION ALL\n"
          + "SELECT DISTINCT ian.eventType, concat(ian.eventType,'-',ian.fieldcode,'-max') as tag from resource_aggregate_numeric ian WHERE ian.hierarchyIdentifier =:hierarchyIdentifier\n", nativeQuery = true)
  List<EntityTagProjection> getUniqueDataTagsAssociatedWithData(String hierarchyIdentifier);



  @Query(value = " SELECT DISTINCT ian.eventType\n"
      + "              , concat(ian.eventType, '-', ian.fieldcode, '-sum') as tag\n"
      + "              , gl.name                                           as geoName\n"
      + "              , cast(gl.identifier as varchar)                    as geoIdentifier\n"
      + "from resource_aggregate_numeric ian\n"
      + "         left join location l on cast(l.identifier as varchar) = ian.locationidentifier\n"
      + "         left join geographic_level gl on gl.identifier = l.geographic_level_identifier\n"
      + "WHERE ian.hierarchyidentifier =  :hierarchyIdentifier\n"
      + "UNION ALL\n"
      + "SELECT DISTINCT ian.eventType\n"
      + "              , concat(ian.eventType, '-', ian.fieldcode, '-median') as tag\n"
      + "              , gl.name                                           as geoName\n"
      + "              , cast(gl.identifier as varchar)                    as geoIdentifier\n"
      + "from resource_aggregate_numeric ian\n"
      + "         left join location l on cast(l.identifier as varchar) = ian.locationidentifier\n"
      + "         left join geographic_level gl on gl.identifier = l.geographic_level_identifier\n"
      + "WHERE ian.hierarchyidentifier =  :hierarchyIdentifier\n"
      + "UNION ALL\n"
      + "SELECT DISTINCT ian.eventType\n"
      + "              , concat(ian.eventType, '-', ian.fieldcode, '-average') as tag\n"
      + "              , gl.name                                           as geoName\n"
      + "              , cast(gl.identifier as varchar)                    as geoIdentifier\n"
      + "from resource_aggregate_numeric ian\n"
      + "         left join location l on cast(l.identifier as varchar) = ian.locationidentifier\n"
      + "         left join geographic_level gl on gl.identifier = l.geographic_level_identifier\n"
      + "WHERE ian.hierarchyidentifier =  :hierarchyIdentifier\n"
      + "UNION ALL\n"
      + "SELECT DISTINCT ian.eventType\n"
      + "              , concat(ian.eventType, '-', ian.fieldcode, '-min') as tag\n"
      + "              , gl.name                                           as geoName\n"
      + "              , cast(gl.identifier as varchar)                    as geoIdentifier\n"
      + "from resource_aggregate_numeric ian\n"
      + "         left join location l on cast(l.identifier as varchar) = ian.locationidentifier\n"
      + "         left join geographic_level gl on gl.identifier = l.geographic_level_identifier\n"
      + "WHERE ian.hierarchyidentifier = :hierarchyIdentifier\n"
      + "UNION ALL\n"
      + "SELECT DISTINCT ian.eventType\n"
      + "              , concat(ian.eventType, '-', ian.fieldcode, '-max') as tag\n"
      + "              , gl.name                                           as geoName\n"
      + "              , cast(gl.identifier as varchar)                    as geoIdentifier\n"
      + "from resource_aggregate_numeric ian\n"
      + "         left join location l on cast(l.identifier as varchar) = ian.locationidentifier\n"
      + "         left join geographic_level gl on gl.identifier = l.geographic_level_identifier\n"
      + "WHERE ian.hierarchyidentifier = :hierarchyIdentifier ",nativeQuery = true)

  List<EntityTagWithGeoLevelAndEntityTypeProjection> getUniqueDataTagsAssociatedWithDataWithLevels(String hierarchyIdentifier);
}
