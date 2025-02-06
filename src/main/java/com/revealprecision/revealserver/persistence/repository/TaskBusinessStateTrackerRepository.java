package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.TaskBusinessStateTracker;
import com.revealprecision.revealserver.persistence.projection.LocationBusinessStateCount;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskBusinessStateTrackerRepository extends
    JpaRepository<TaskBusinessStateTracker, UUID> {

  List<TaskBusinessStateTracker> findTaskBusinessStateTrackerByLocationHierarchyIdentifierAndTaskLocationIdentifierAndPlanIdentifier(
      UUID locationHierarchyIdentifier, UUID taskLocationIdentifier, UUID planIdentifier);


  @Query(
      "SELECT DISTINCT new TaskBusinessStateTracker (t.taskLocationIdentifier,t.parentGeographicLevelName,t.taskLocationName,t.taskBusinessStatus) from TaskBusinessStateTracker t WHERE t.planIdentifier = :planIdentifier "
          + "and t.locationHierarchyIdentifier = :locationHierarchyIdentifier and t.taskLocationIdentifier = :taskLocationIdentifier")
  TaskBusinessStateTracker findDistinctTaskBusinessStateTrackerByLocationHierarchyIdentifierAndTaskLocationIdentifierAndPlanIdentifier(
      UUID locationHierarchyIdentifier, UUID taskLocationIdentifier, UUID planIdentifier);

  @Query(
          value = "SELECT DISTINCT t.task_business_status from task_business_state_tracker t WHERE t.plan_identifier = '24e8d653-c939-4de9-9e02-0246126e33d2'\n" +
                  "         and t.task_location_identifier = '220b31f3-421e-4a94-864d-1de9642f1163' and t.location_hierarchy_identifier = 'b88451af-5759-4e01-ad4e-9b34d21c958a'", nativeQuery = true)
  String findDistinctStateTracerBTaskLocationId(
          UUID locationHierarchyIdentifier, UUID taskLocationIdentifier, UUID planIdentifier);

  @Query(
      "SELECT t.parentLocationIdentifier as parentLocationIdentifier, t.planIdentifier as planIdentifier, count(t) as locationCount from TaskBusinessStateTracker t "
          + "where t.parentLocationIdentifier = :parentLocationIdentifier"
          + " and t.taskLocationGeographicLevelName = :taskLocationGeographicLevelName and "
          + "t.planIdentifier = :planIdentifier and t.locationHierarchyIdentifier = :locationHierarchyIdentifier and t.taskBusinessStatus = :taskBusinessStatus"
          + " group by t.parentLocationIdentifier, t.planIdentifier ")
  LocationBusinessStateCount getLocationBusinessStateObjPerBusinessStatusAndGeoLevel(
      UUID planIdentifier, UUID parentLocationIdentifier, String taskLocationGeographicLevelName,
      String taskBusinessStatus, UUID locationHierarchyIdentifier);

  @Query(
      "SELECT t.parentLocationIdentifier as parentLocationIdentifier, t.planIdentifier as planIdentifier,t.taskBusinessStatus as taskBusinessStatus, count(t) as locationCount from TaskBusinessStateTracker t "
          + "where t.parentLocationIdentifier = :parentLocationIdentifier"
          + " and t.taskLocationGeographicLevelName = :taskLocationGeographicLevelName and "
          + "t.planIdentifier = :planIdentifier and t.locationHierarchyIdentifier = :locationHierarchyIdentifier"
          + " group by t.parentLocationIdentifier, t.planIdentifier,t.taskBusinessStatus ")
  Set<LocationBusinessStateCount> getLocationBusinessStateObjPerGeoLevel(UUID planIdentifier,
      UUID parentLocationIdentifier, String taskLocationGeographicLevelName,UUID locationHierarchyIdentifier);
}
