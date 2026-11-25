package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.aggregation.ImportAggregationString;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportAggregationStringRepository extends
    JpaRepository<ImportAggregationString, Integer> {

  Optional<ImportAggregationString> findByNameAndAncestorAndFieldCode(String name, String ancestor,
      String fieldCode);

  Optional<ImportAggregationString> findByNameAndAncestorAndFieldCodeAndHierarchyIdentifier(String name, String ancestor,
      String fieldCode, String hierarchIdentifier);


  Optional<ImportAggregationString> findByNameAndAncestorAndFieldCodeAndHierarchyIdentifierAndDataCaptureDate(String name, String ancestor,
      String fieldCode, String hierarchIdentifier, LocalDate dataCaptureDate);

  // planIdentifier holds the instance identifier for 'user-set' rows and is null for dataset
  // imports; a null dataCaptureDate / planIdentifier is matched with IS NULL by the derived query
  Optional<ImportAggregationString> findByNameAndAncestorAndFieldCodeAndHierarchyIdentifierAndDataCaptureDateAndPlanIdentifier(
      String name, String ancestor, String fieldCode, String hierarchIdentifier,
      LocalDate dataCaptureDate, String planIdentifier);

}
