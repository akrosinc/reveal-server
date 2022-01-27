package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.LookupInterventionType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LookupInterventionTypeRepository extends
    JpaRepository<LookupInterventionType, UUID> {

  @Query(value = "select it from LookupInterventionType it where lower(it.name) = lower(:name) or lower(it.code) = lower(:code)")
  Optional<LookupInterventionType> findByNameOrCode(@Param("name") String name,
      @Param("code") String code);
}
