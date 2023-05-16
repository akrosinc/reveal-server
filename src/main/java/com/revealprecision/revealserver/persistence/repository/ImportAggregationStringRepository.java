package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.aggregation.ImportAggregationString;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportAggregationStringRepository extends
    JpaRepository<ImportAggregationString, Integer> {

  Optional<ImportAggregationString> findByNameAndAncestorAndFieldCode(String name, String ancestor,
      String fieldCode);
}
