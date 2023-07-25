package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.aggregation.ResourceAggregationNumeric;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceAggregationNumericRepository extends
    JpaRepository<ResourceAggregationNumeric, Integer> {

  List<ResourceAggregationNumeric> findResourceAggregationNumericByHierarchyIdentifierAndAncestorInAndResourcePlanName(
      String hierarchyIdentifier, List<String> locationIdentifier, String resourcePlanName);
}
