package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Condition;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConditionRepository extends JpaRepository<Condition, UUID> {

  @Query(value = "select c from Condition c where c.action.identifier = :actionIdentifier")
  Page<Condition> getAllByPlanId(@Param("actionIdentifier") UUID actionIdentifier,
      Pageable pageable);
}
