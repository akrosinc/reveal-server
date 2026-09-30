package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.RasterLocationZonalStats;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RasterLocationZonalStatsRepository extends JpaRepository<RasterLocationZonalStats, UUID> {

  Optional<RasterLocationZonalStats> findByRasterIdAndLocation_Identifier(String rasterId, UUID locationIdentifier);

  List<RasterLocationZonalStats> findByRasterId(String rasterId);

  @Query("SELECT r.location.identifier FROM RasterLocationZonalStats r WHERE r.rasterId = :rasterId")
  Set<UUID> findLocationIdentifiersByRasterId(@Param("rasterId") String rasterId);

  boolean existsByRasterIdAndLocation_Identifier(String rasterId, UUID locationIdentifier);

  void deleteByRasterId(String rasterId);
}
