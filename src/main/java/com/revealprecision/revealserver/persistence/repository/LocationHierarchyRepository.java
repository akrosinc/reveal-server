package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LocationHierarchyRepository extends JpaRepository<LocationHierarchy, UUID> {

  @Query(value = "SELECT * FROM location_hierarchy l where l.node_order = CAST(:nodes AS VARCHAR[])", nativeQuery = true)
  List<LocationHierarchy> findByNodeOrderArray(@Param("nodes") String nodeOrder);
}
