package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.LocationStats;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationStatsRepository extends JpaRepository<LocationStats, UUID> {

  Optional<LocationStats> findByDatasetIdAndLocation_Identifier(String datasetId, UUID locationIdentifier);

  List<LocationStats> findByDatasetId(String datasetId);

  @Query("SELECT r.location.identifier FROM LocationStats r WHERE r.datasetId = :datasetId")
  Set<UUID> findLocationIdentifiersByDatasetId(@Param("datasetId") String datasetId);

  boolean existsByDatasetIdAndLocation_Identifier(String datasetId, UUID locationIdentifier);

  void deleteByDatasetId(String datasetId);
}
