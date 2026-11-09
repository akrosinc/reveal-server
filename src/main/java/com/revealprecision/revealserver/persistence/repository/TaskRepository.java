package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.projection.TaskProjection;
import com.revealprecision.revealserver.persistence.projection.TaskSyncProjection;
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

  List<Task> findByIdentifierIn(List<UUID> identifiers);

  List<Task> findTasksByAction_IdentifierAndLocation_Identifier(UUID actionIdentifier,
      UUID locationIdentifier);

  @Query("select count(t) > 0 from Task t "
      + "where t.action.identifier = :actionIdentifier "
      + "and t.location.identifier = :locationIdentifier "
      + "and t.authoredOn >= :windowStart and t.authoredOn < :windowEnd")
  boolean existsForActionAndLocationInWindow(
      @Param("actionIdentifier") UUID actionIdentifier,
      @Param("locationIdentifier") UUID locationIdentifier,
      @Param("windowStart") java.time.LocalDateTime windowStart,
      @Param("windowEnd") java.time.LocalDateTime windowEnd);

  List<Task> findTasksByPlan_Identifier(UUID planIdentifier);
  Page<Task> findTasksByPlan_Identifier(UUID planIdentifier, Pageable pageable);

  List<Task> findAllByBaseEntityIdentifierIn(List<UUID> uuids);

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
      "SELECT new com.revealprecision.revealserver.persistence.domain.Task(t.identifier,t.plan.identifier,t.serverVersion,t.taskFacade, lr.parentLocation.identifier, t.location)from  Task t "
          + "left join Location l on l = t.location "
          + "left join Plan p on p = t.plan "
          + "left join LocationRelationship  lr on lr.location = l "
          + "WHERE p.identifier = :planIdentifier and lr.parentLocation.identifier in :locationIdentifiers and t.serverVersion >= :serverVersion ")
  List<Task> getStructureTaskFacadesByLocationServerVersionAndPlan(UUID planIdentifier,
      List<UUID> locationIdentifiers, Long serverVersion);

  @Query(
      "SELECT new com.revealprecision.revealserver.persistence.domain.Task(t.identifier,t.plan.identifier,t.serverVersion,t.taskFacade, lr.parentLocation.identifier, t.location)from  Task t "
          + "inner join Person per on per.identifier = t.person.identifier "
          + "left join per.locations l "
          + "left join Plan p on p = t.plan "
          + "left join LocationRelationship  lr on lr.location.identifier = l.identifier "
          + "WHERE p.identifier = :planIdentifier and lr.parentLocation.identifier in :locationIdentifiers and t.serverVersion >= :serverVersion ")
  List<Task> getStructureForPeopleTaskFacadesByLocationServerVersionAndPlan(UUID planIdentifier,
      List<UUID> locationIdentifiers, Long serverVersion);


  /**
   * Tasks visible to a set of organizations: for every SELECTED plan_assignment of the
   * organizations (restricted to the requested, ACTIVE plans) return tasks of that plan located on
   * the selected location or on any descendant of it. Descendants come from the ancestry array on
   * location_relationship (GIN indexed), so there is no recursive walk. Person tasks are matched
   * through the person's locations.
   */
  @Query(value = "WITH scope AS ( "
      + "  SELECT pl.plan_identifier AS plan_identifier, pl.location_identifier AS location_identifier "
      + "  FROM plan_assignment pa "
      + "  JOIN plan_locations pl ON pl.identifier = pa.plan_locations_identifier "
      + "  JOIN plan p ON p.identifier = pl.plan_identifier "
      + "  WHERE pa.organization_identifier IN (:organizationIds) "
      + "    AND pa.selected = true AND pa.entity_status = 'ACTIVE' "
      + "    AND pl.entity_status = 'ACTIVE' "
      + "    AND p.status = 'ACTIVE' AND p.entity_status = 'ACTIVE' "
      + "    AND pl.plan_identifier IN (:planIds) "
      + "), scope_locations AS ( "
      + "  SELECT s.plan_identifier, s.location_identifier FROM scope s "
      + "  UNION "
      + "  SELECT s.plan_identifier, lr.location_identifier FROM scope s "
      + "  JOIN location_relationship lr ON lr.ancestry @> ARRAY[s.location_identifier] "
      + "  WHERE lr.entity_status = 'ACTIVE' "
      + "), hits AS ( "
      + "  SELECT tl.task_identifier, sl.plan_identifier, tl.location_identifier "
      + "  FROM task_location tl "
      + "  JOIN scope_locations sl ON sl.location_identifier = tl.location_identifier "
      + "  UNION "
      + "  SELECT tp.task_identifier, sl.plan_identifier, pl.location_identifier "
      + "  FROM task_person tp "
      + "  JOIN person_location pl ON pl.person_identifier = tp.person_identifier "
      + "  JOIN scope_locations sl ON sl.location_identifier = pl.location_identifier "
      + ") "
      + "SELECT DISTINCT ON (t.identifier) "
      + "  cast(t.identifier as varchar) AS identifier, "
      + "  t.server_version AS serverVersion, "
      + "  cast(t.task_facade as text) AS taskFacade, "
      + "  cast(h.location_identifier as varchar) AS locationIdentifier "
      + "FROM hits h "
      + "JOIN task t ON t.identifier = h.task_identifier AND t.plan_identifier = h.plan_identifier "
      + "WHERE t.entity_status = 'ACTIVE' AND t.task_facade IS NOT NULL "
      + "  AND t.server_version >= :serverVersion "
      + "ORDER BY t.identifier", nativeQuery = true)
  List<TaskSyncProjection> getTasksForAssignedOrganizations(
      @Param("organizationIds") List<UUID> organizationIds,
      @Param("planIds") List<UUID> planIds,
      @Param("serverVersion") Long serverVersion);

  @Query(value = "SELECT DISTINCT cast(t.identifier as varchar)  from task t\n"
      + "left join task_business_state_tracker tbst on t.base_entity_identifier = tbst.task_location_identifier\n"
      + "WHERE t.business_status != 'Not Visited' and t.business_status != tbst.task_business_status", nativeQuery = true)
  List<String> findTasksNotSameAsInTaskBusinessStateTracker();

  @Query(value = "SELECT  DISTINCT cast(t.identifier as varchar)  from task t \n"
      + "left join task_business_state_tracker tbst on t.base_entity_identifier = tbst.task_location_identifier\n"
      + "\n"
      + "WHERE t.business_status != 'Not Visited' and tbst.identifier is null", nativeQuery = true)
  List<String> findTasksByNotPresentInTaskBusinessStateTracker();

  @Query(value =
      "SELECT COUNT(tl.location_identifier) " +
          "FROM task t " +
          "JOIN task_location tl ON tl.task_identifier = t.identifier " +
          "WHERE t.plan_identifier = :planId " +
          "AND t.entity_status = 'ACTIVE'",
      nativeQuery = true)
  Long countTotalTaskLocationsByPlanId(@Param("planId") UUID planId);

  @Query(value =
      "SELECT COUNT(tl.location_identifier) " +
          "FROM task t " +
          "JOIN task_location tl ON tl.task_identifier = t.identifier " +
          "JOIN lookup_task_status lts ON lts.identifier = t.lookup_task_status_identifier " +
          "WHERE t.plan_identifier = :planId " +
          "AND lts.code = 'COMPLETED' " +
          "AND t.entity_status = 'ACTIVE'",
      nativeQuery = true)
  Long countCompletedTaskLocationsByPlanId(@Param("planId") UUID planId);

}
