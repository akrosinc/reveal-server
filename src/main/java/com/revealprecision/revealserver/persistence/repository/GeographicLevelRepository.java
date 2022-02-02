package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GeographicLevelRepository extends JpaRepository<GeographicLevel, UUID> {

  Optional<GeographicLevel> findByName(String name);

  @Query("select gl.name from GeographicLevel gl where gl.name in :names")
  List<String> getNames(@Param("names") List<String> names);
}
