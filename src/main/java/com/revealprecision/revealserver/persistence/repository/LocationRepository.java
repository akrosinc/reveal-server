package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Location;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {

  List<Location> findByGeographicLevelIdentifier(UUID geographicLevelId);

  @Query(value =
      "SELECT * FROM location l WHERE (lower(l.name) like lower(concat('%', :param, '%'))) AND l.entity_status='ACTIVE'", nativeQuery = true)
  Page<Location> findAlLByCriteria(@Param("param") String param, Pageable pageable);

  @Query(value = "SELECT COUNT(*) FROM location l WHERE (lower(l.name) like lower(concat('%', :param, '%'))) AND l.entity_status='ACTIVE'", nativeQuery = true)
  long findAllCountByCriteria(@Param("param") String param);
}
