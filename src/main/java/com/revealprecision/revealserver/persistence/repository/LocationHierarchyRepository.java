package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LocationHierarchyRepository extends JpaRepository<LocationHierarchy, UUID> {
}
