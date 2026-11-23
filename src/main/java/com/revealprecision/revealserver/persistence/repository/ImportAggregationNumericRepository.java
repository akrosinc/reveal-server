package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.aggregation.ImportAggregationNumeric;
import com.revealprecision.revealserver.persistence.projection.LocationEntityTagCountProjection;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportAggregationNumericRepository extends
    JpaRepository<ImportAggregationNumeric, Integer> {

  Optional<ImportAggregationNumeric> findByNameAndAncestorAndFieldCode(String name, String ancestor,
      String fieldCode);

  Optional<ImportAggregationNumeric> findByNameAndAncestorAndFieldCodeAndHierarchyIdentifier(String name, String ancestor,
      String fieldCode, String hierarchIdentifier);

  void deleteAllByFieldCodeIn(List<String> fieldCodes);

  List<ImportAggregationNumeric> findAllByFieldCodeIn(List<String> fieldCodes);

  void removeAllByFieldCodeIn(List<String> fieldCodes);

  Optional<ImportAggregationNumeric> findByNameAndAncestorAndFieldCodeAndHierarchyIdentifierAndDataCaptureDate(
      String name,
      String locId, String tag, String hierarchyIdentifier, LocalDate dataCaptureDate);

  // values of the given tags for a location (rows whose ancestor is the location): count is the
  // location's own value (row named after the location), total includes its descendants
  @Query("SELECT ag.fieldCode AS tag, "
      + "SUM(CASE WHEN ag.name = :name THEN ag.val ELSE NULL END) AS ownCount, "
      + "SUM(ag.val) AS totalCount "
      + "FROM ImportAggregationNumeric ag "
      + "WHERE ag.ancestor = :ancestor AND ag.hierarchyIdentifier = :hierarchyIdentifier "
      + "AND ag.fieldCode IN :tags AND ag.dataCaptureDate = :dataCaptureDate "
      + "GROUP BY ag.fieldCode")
  List<LocationEntityTagCountProjection> findTagCountsByLocationAndDataCaptureDate(
      @Param("ancestor") String ancestor, @Param("name") String name,
      @Param("hierarchyIdentifier") String hierarchyIdentifier,
      @Param("tags") Collection<String> tags, @Param("dataCaptureDate") LocalDate dataCaptureDate);

  @Query("SELECT ag.fieldCode AS tag, "
      + "SUM(CASE WHEN ag.name = :name THEN ag.val ELSE NULL END) AS ownCount, "
      + "SUM(ag.val) AS totalCount "
      + "FROM ImportAggregationNumeric ag "
      + "WHERE ag.ancestor = :ancestor AND ag.hierarchyIdentifier = :hierarchyIdentifier "
      + "AND ag.fieldCode IN :tags AND ag.dataCaptureDate IS NULL "
      + "GROUP BY ag.fieldCode")
  List<LocationEntityTagCountProjection> findTagCountsByLocationWithoutDataCaptureDate(
      @Param("ancestor") String ancestor, @Param("name") String name,
      @Param("hierarchyIdentifier") String hierarchyIdentifier,
      @Param("tags") Collection<String> tags);
}
