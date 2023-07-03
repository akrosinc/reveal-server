package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.aggregation.EventAggregationNumeric;
import com.revealprecision.revealserver.persistence.projection.EventAggregateNumericProjection;
import com.revealprecision.revealserver.persistence.projection.EventAggregateStringCountProjection;
import com.revealprecision.revealserver.persistence.projection.EventAggregationNumericTagProjection;
import java.util.List;
import java.util.UUID;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventAggregateRepository extends JpaRepository<EventAggregationNumeric, UUID> {



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
  List<EventAggregateNumericProjection> getAggregationValuesByLocationList(
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
  List<EventAggregateStringCountProjection> getAggregationCountValuesByLocationList(
      @Param("locationIdentifiers") List<String> locationIdentifiers);

  @Query(value = "SELECT distinct\n"
      + "   ean.event_type as eventType,\n"
      + "    ean.fieldcode as fieldcode \n"
      + "                 from event_aggregation_numeric ean", nativeQuery = true)
  List<EventAggregationNumericTagProjection> getUniqueTagsFromEventAggregationNumeric();

  @Query(value = "SELECT distinct\n"
      + "   ean.event_type as eventType,\n"
      + "    ean.fieldcode as fieldcode \n"
      + "  from event_aggregation_string_count ean", nativeQuery = true)
  List<EventAggregationNumericTagProjection> getUniqueTagsFromEventAggregationStringCount();


  @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY event_aggregate_string_count", nativeQuery = true)
  @Transactional
  @Modifying
  void refreshImportAggregateStringCountMaterializedView();

  @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY event_aggregate_numeric", nativeQuery = true)
  @Transactional
  @Modifying
  void refreshImportAggregateNumericMaterializedView();

}
