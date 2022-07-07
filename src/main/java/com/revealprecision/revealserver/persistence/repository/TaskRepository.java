package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.projection.TaskProjection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID>,
    JpaSpecificationExecutor<Task> {

  Optional<Task> findByIdentifier(UUID identifier);

  List<Task> findTasksByAction_IdentifierAndLocation_Identifier(UUID actionIdentifier,
      UUID locationIdentifier);

  List<Task> findTasksByPlan_Identifier(UUID planIdentifier);

  @Query("select t from Task t where t.plan = :plan and t.baseEntityIdentifier in :baseEntityIdentifiers and t.serverVersion >= :serverVersion")
  List<Task> findByPlanAndBaseEntityIdentifiersAndMinimumServerVersion(@Param("plan") Plan plan,
      @Param("baseEntityIdentifiers") List<UUID> baseEntityIdentifiers,
      @Param("serverVersion") Long serverVersion);

  @Query("SELECT DISTINCT t.identifier as identifier, t.baseEntityIdentifier as baseEntityIdentifier FROM Task t WHERE  t.plan = :plan AND t.action.identifier = :actionIdentifier")
  List<TaskProjection> findUniqueByPlanAndActionidentifier(@Param("plan") Plan plan,
      UUID actionIdentifier);

}