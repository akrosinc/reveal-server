package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.LocationCounts;
import java.util.UUID;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationCountsRepository extends JpaRepository<LocationCounts, UUID> {


  @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY location_counts ", nativeQuery = true)
  @Modifying
  @Transactional
  void refreshLocationCountsMaterializedView();

  LocationCounts findLocationCountsByParentLocationIdentifierAndLocationHierarchyIdentifierAndGeographicLevelName(UUID parentLocationIdentifier, UUID locationHierarchyIdentifier, String geographicLevelName);


}
