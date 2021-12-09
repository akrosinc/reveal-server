package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationHierarchyRepository extends JpaRepository<LocationHierarchy, UUID> {
  List<LocationHierarchy> findByNodeOrderArray(String nodeOrder);
}
