package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.persistence.domain.Action;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionRepository extends EntityGraphJpaRepository<Action, UUID> {

  @Query(value = "select a from Action a where a.goal.identifier = :identifier")
  Page<Action> getActions(@Param("identifier") UUID identifier, Pageable pageable);

  List<Action> findActionsByGoal_Identifier(UUID goalIdentifier);

  Action findByTitle(String title);

  Action findByTitleAndGoal_Plan_Identifier(String title, UUID planIdentifier);
}
