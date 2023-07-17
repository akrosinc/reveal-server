package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.aggregation.ImportAggregationNumeric;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportAggregationNumericRepository extends
    JpaRepository<ImportAggregationNumeric, Integer> {

  Optional<ImportAggregationNumeric> findByNameAndAncestorAndFieldCode(String name, String ancestor,
      String fieldCode);

  Optional<ImportAggregationNumeric> findByNameAndAncestorAndFieldCodeAndHierarchyIdentifier(String name, String ancestor,
      String fieldCode, String hierarchIdentifier);
}
