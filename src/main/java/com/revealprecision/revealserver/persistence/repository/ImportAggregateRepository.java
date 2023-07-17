package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.aggregation.ImportAggregationNumeric;
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

  @Query(value = "SELECT ean2.hierarchyidentifier as hierarchyIdentifier, ean2.name as name,\n"
      + "       cast(ean2.locationIdentifier as varchar) as locationIdentifier,\n"
      + "       cast(ean2.planIdentifier as varchar) as planIdentifier,\n"
      + "       ean2.eventType as eventType,\n"
      + "       ean2.fieldcode as fieldCode,\n"
      + "       ean2.sum as sum,\n"
      + "       ean2.avg as avg,\n"
      + "       ean2.median as median\n"
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

}
