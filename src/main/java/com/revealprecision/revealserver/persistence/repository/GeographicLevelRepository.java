package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeographicLevelRepository extends JpaRepository<GeographicLevel, UUID> {

  Optional<GeographicLevel> findByName(String name);
}
