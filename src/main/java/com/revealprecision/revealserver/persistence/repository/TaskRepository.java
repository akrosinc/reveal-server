package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.projection.TaskProjection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
  Page<Task> findTasksByPlan_Identifier(UUID planIdentifier, Pageable pageable);

  @Query("select t from Task t where t.plan = :plan and t.baseEntityIdentifier in :baseEntityIdentifiers and t.serverVersion >= :serverVersion")
  List<Task> findByPlanAndBaseEntityIdentifiersAndMinimumServerVersion(@Param("plan") Plan plan,
      @Param("baseEntityIdentifiers") List<UUID> baseEntityIdentifiers,
      @Param("serverVersion") Long serverVersion);

  @Query("SELECT DISTINCT t.identifier as identifier, t.baseEntityIdentifier as baseEntityIdentifier FROM Task t WHERE  t.plan = :plan AND t.action.identifier = :actionIdentifier")
  List<TaskProjection> findUniqueByPlanAndActionidentifier(@Param("plan") Plan plan,
      UUID actionIdentifier);

  @Query(
      "SELECT new com.revealprecision.revealserver.persistence.domain.Task(t.identifier,t.plan.identifier,t.serverVersion,t.taskFacade, lr.parentLocation.identifier) from  Task t "
          + "left join Location l on l = t.location left join Plan p on p = t.plan "
          + "left join LocationRelationship  lr on lr.location = l "
          + "WHERE p.identifier = :planIdentifier and t.location.identifier in :locationIdentifiers and t.serverVersion >= :serverVersion")
  List<Task> getNonStructureTaskFacadesByLocationServerVersionAndPlan(UUID planIdentifier,
      List<UUID> locationIdentifiers, Long serverVersion);

  @Query(
      "SELECT new com.revealprecision.revealserver.persistence.domain.Task(t.identifier,t.plan.identifier,t.serverVersion,t.taskFacade, lr.parentLocation.identifier)from  Task t "
          + "left join Location l on l = t.location "
          + "left join Plan p on p = t.plan "
          + "left join LocationRelationship  lr on lr.location = l "
          + "WHERE p.identifier = :planIdentifier and lr.parentLocation.identifier in :locationIdentifiers and t.serverVersion >= :serverVersion ")
  List<Task> getStructureTaskFacadesByLocationServerVersionAndPlan(UUID planIdentifier,
      List<UUID> locationIdentifiers, Long serverVersion);


  @Query(value = "SELECT DISTINCT cast(t.identifier as varchar)  from task t\n"
      + "left join task_business_state_tracker tbst on t.base_entity_identifier = tbst.task_location_identifier\n"
      + "WHERE t.business_status != 'Not Visited' and t.business_status != tbst.task_business_status", nativeQuery = true)
  List<String> findTasksNotSameAsInTaskBusinessStateTracker();

  @Query(value = "SELECT  DISTINCT cast(t.identifier as varchar)  from task t \n"
      + "left join task_business_state_tracker tbst on t.base_entity_identifier = tbst.task_location_identifier\n"
      + "\n"
      + "WHERE t.business_status != 'Not Visited' and tbst.identifier is null",nativeQuery = true)
  List<String> findTasksByNotPresentInTaskBusinessStateTracker();


}