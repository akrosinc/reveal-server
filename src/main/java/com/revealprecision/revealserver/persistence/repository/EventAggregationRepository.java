package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.aggregation.EventAggregationNumeric;
import com.revealprecision.revealserver.persistence.projection.EventAggregationNumericProjection;
import com.revealprecision.revealserver.persistence.projection.EventAggregationNumericTagProjection;
import com.revealprecision.revealserver.persistence.projection.EventAggregationStringCountProjection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventAggregationRepository extends JpaRepository<EventAggregationNumeric, UUID> {


  @Query(value = "SELECT ean2.name as name,\n"
      + "       cast(ean2.ancestor as varchar) as locationIdentifier,\n"
      + "       cast(ean2.plan_identifier as varchar) as planIdentifier,\n"
      + "       ean2.event_type as eventType,\n"
      + "       ean2.fieldcode as fieldCode,\n"
      + "       sum(cast(ean2.val as float8)) as sum,\n"
      + "       avg(cast(ean2.val as float8)) as avg,\n"
      + "       percentile_cont(0.5) WITHIN GROUP ( ORDER BY cast(ean2.val as float8) ) as median\n"
      + "from event_aggregation_numeric ean2\n"
      + "\n"
      + "WHERE cast(ean2.ancestor as varchar) = :locationIdentifier\n"
      + "group by ean2.name,ean2.ancestor, ean2.plan_identifier, ean2.event_type, ean2.fieldcode\n", nativeQuery = true)
  List<EventAggregationNumericProjection> getAggregationValuesByLocation(
      @Param("locationIdentifier") String locationIdentifier);


  @Query(value = "SELECT ean2.name as name,\n"
      + "       cast(ean2.locationIdentifier as varchar) as locationIdentifier,\n"
      + "       cast(ean2.planIdentifier as varchar) as planIdentifier,\n"
      + "       ean2.eventType as eventType,\n"
      + "       ean2.fieldcode as fieldCode,\n"
      + "       ean2.sum as sum,\n"
      + "       ean2.avg as avg,\n"
      + "       ean2.median as median\n"
      + "from event_aggregate_numeric ean2\n"
      + "\n"
      + "WHERE cast(ean2.locationIdentifier as varchar) in :locationIdentifiers", nativeQuery = true)
  List<EventAggregationNumericProjection> getAggregationValuesByLocationList(
      @Param("locationIdentifiers") List<String> locationIdentifiers);


  @Query(value = "SELECT ean2.name as name,\n"
      + "       cast(ean2.locationIdentifier as varchar) as locationIdentifier,\n"
      + "       cast(ean2.planIdentifier as varchar) as planIdentifier,\n"
      + "       ean2.eventType as eventType,\n"
      + "       ean2.fieldCode as fieldCode,\n"
      + "       ean2.fieldVal as fieldVal,\n"
      + "       ean2.count as count\n"
      + "from event_aggregate_string_count ean2\n"
      + "WHERE cast(ean2.locationIdentifier as varchar) in :locationIdentifiers\n", nativeQuery = true)
  List<EventAggregationStringCountProjection> getAggregationCountValuesByLocationList(
      @Param("locationIdentifiers") List<String> locationIdentifiers);

  @Query("SELECT DISTINCT l.identifier from Location l INNER JOIN EventAggregationNumeric ean on l.identifier = ean.ancestor")
  Page<UUID> getLocationsWithNumericMetadata(Pageable pageable);

  @Query("SELECT DISTINCT l.identifier from Location l INNER JOIN EventAggregationStringCount ean on l.identifier = ean.ancestor")
  Page<UUID> getLocationsWithStringMetadata(Pageable pageable);

//  @Query(value = "SELECT distinct\n"
//      + "    concat(ean.event_type,'_',ean.fieldcode,'_sum') as eventTagSum,\n"
//      + "    concat(ean.event_type,'_',ean.fieldcode,'_average') as eventTagAverage,\n"
//      + "                concat(ean.event_type,'_',ean.fieldcode,'_median') as eventTagMedian from event_aggregation_numeric_2 ean",nativeQuery = true)
//  List<EventAggregationNumericTagProjection> getUniqueTagsFromEventAggregationNumeric();
//
//  @Query(value = "SELECT distinct\n"
//      + "    concat(ean.event_type,'_',ean.fieldcode,'_',ean.val,'_count') as eventTagCount\n"
//      + "  from event_aggregation_string_count_2 ean",nativeQuery = true)
//  List<String> getUniqueTagsFromEventAggregationStringCount();

  @Query(value = "SELECT distinct\n"
      + "    concat(ean.event_type,'-',ean.fieldcode,'-sum') as eventTagSum,\n"
      + "    concat(ean.event_type,'-',ean.fieldcode,'-average') as eventTagAverage,\n"
      + "                concat(ean.event_type,'-',ean.fieldcode,'-median') as eventTagMedian from event_aggregation_numeric ean", nativeQuery = true)
  List<EventAggregationNumericTagProjection> getUniqueTagsFromEventAggregationNumeric();

  @Query(value = "SELECT distinct\n"
      + "    concat(ean.event_type,'-',ean.fieldcode,'-',ean.val,'-count') as eventTagCount\n"
      + "  from event_aggregation_string_count ean", nativeQuery = true)
  List<String> getUniqueTagsFromEventAggregationStringCount();
}
