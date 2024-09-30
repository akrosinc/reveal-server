package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.constants.FormConstants.BusinessStatus;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.enums.LookupEntityTypeCodeEnum;
import com.revealprecision.revealserver.enums.PlanInterventionTypeEnum;
import com.revealprecision.revealserver.messaging.message.TaskEvent;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationAboveStructure;
import com.revealprecision.revealserver.persistence.domain.TaskBusinessStateTracker;
import com.revealprecision.revealserver.persistence.projection.LocationBusinessStateCount;
import com.revealprecision.revealserver.persistence.repository.TaskBusinessStateTrackerRepository;
import com.revealprecision.revealserver.service.LocationBusinessStatusService;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.PlanLocationsService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("KafkaMessaging & (Listening | task-listener)")
public class TaskListener extends Listener {

  private final TaskBusinessStateTrackerRepository taskBusinessStateTrackerRepository;
  private final LocationRelationshipService locationRelationshipService;
  private final LocationService locationService;
  private final LocationBusinessStatusService locationBusinessStatusService;
  private final PlanLocationsService planLocationsService;

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('TASK')}", groupId = "reveal_server_group")
  public void listenGroupFoo(TaskEvent message) {
    log.info("Received Message in group foo: {}", message.toString());
    init();

    if (message.getAction().getLookupEntityType().getCode()
        .equals(LookupEntityTypeCodeEnum.PERSON_CODE.getLookupEntityType())) {
      return;
    }
    List<TaskBusinessStateTracker> taskBusinessStateTrackers = taskBusinessStateTrackerRepository.findTaskBusinessStateTrackerByLocationHierarchyIdentifierAndTaskLocationIdentifierAndPlanIdentifier(
        message.getAction().getGoal().getPlan().getLocationHierarchy().getIdentifier(),
        UUID.fromString(message.getLocationId()),
        message.getAction().getGoal().getPlan().getIdentifier());

    List<TaskBusinessStateTracker> trackers;

    if (!taskBusinessStateTrackers.isEmpty()) {
      trackers = updateTaskBusinessStateTrackers(message, taskBusinessStateTrackers);
    } else {
      trackers = createBusinessStateTrackers(message);
    }
    taskBusinessStateTrackerRepository.saveAll(trackers);

    saveDataForLocationsAboveStructure(message, trackers);
  }

  private List<TaskBusinessStateTracker> createBusinessStateTrackers(TaskEvent message) {
    List<TaskBusinessStateTracker> trackers = new ArrayList<>();
    try {
      List<UUID> ancestry = locationRelationshipService.getAncestryForLocation(
          UUID.fromString(message.getLocationId()),message.getAction().getGoal().getPlan().getLocationHierarchy().getIdentifier());

      String locationGeographicLevelName;
      String locationName;
      if (message.getLocationGeographicLevelName() == null || message.getLocationName() == null) {
        Location location = locationService.findByIdentifier(
            UUID.fromString(message.getLocationId()));
        locationGeographicLevelName = location.getGeographicLevel().getName();
        locationName = location.getName();
      } else {
        locationGeographicLevelName = message.getLocationGeographicLevelName();
        locationName = message.getLocationName();
      }

      if (ancestry != null) {
        trackers = ancestry
            .stream().map(ancestor -> {

              Location locationAncestor = locationService.findByIdentifier(ancestor);

              return getTaskBusinessStateTracker(message, locationGeographicLevelName, locationName,
                  locationAncestor);
            }).collect(Collectors.toList());

      }
    } catch (Exception e){
      log.error("Unable to create task tracker for "+message,e);
    }
    return trackers;
  }

  private List<TaskBusinessStateTracker> updateTaskBusinessStateTrackers(TaskEvent message,
      List<TaskBusinessStateTracker> taskBusinessStateTrackers) {
    List<TaskBusinessStateTracker> trackers;
    trackers = taskBusinessStateTrackers.stream()
        .peek(taskBusinessStateTracker1 -> {
          if (message.getLastUpdated()
              .isAfter(taskBusinessStateTracker1.getTaskBusinessStatusUpdateTime())) {
            taskBusinessStateTracker1.setTaskBusinessStatus(message.getBusinessStatus());
            taskBusinessStateTracker1.setTaskBusinessStatusUpdateTime(
                message.getLastUpdated());
          }
        }).collect(Collectors.toList());

    taskBusinessStateTrackerRepository.saveAll(trackers);
    return trackers;
  }

  private void saveDataForLocationsAboveStructure(TaskEvent message,
      List<TaskBusinessStateTracker> trackers) {
    if (message.getAction().getGoal().getPlan().getPlanTargetTypeEvent().getGeographicLevelName()
        .equals(
            LocationConstants.STRUCTURE)) {
      List<String> nodeOrder = message.getAction().getGoal().getPlan().getLocationHierarchy()
          .getNodeOrder();
      String node = nodeOrder.get(nodeOrder.indexOf(LocationConstants.STRUCTURE) - 1);

      List<Location> parentsAboveNodeAboveStructure = IntStream.range(0,
              nodeOrder.indexOf(LocationConstants.STRUCTURE) - 1)
          .mapToObj(nodeOrder::get)
          .map(
              node1 -> trackers.stream()
                  .filter(tracker -> tracker.getParentGeographicLevelName().equals(node1))
                  .findFirst()
                  .get())
          .map(taskBusinessStateTracker ->
              Location.builder()
                  .identifier(taskBusinessStateTracker.getParentLocationIdentifier())
                  .name(taskBusinessStateTracker.getParentLocationName())
                  .geographicLevel(GeographicLevel.builder()
                      .name(taskBusinessStateTracker.getParentGeographicLevelName()).build())
                  .build())
          .collect(Collectors.toList());

      Optional<TaskBusinessStateTracker> first = trackers.stream().filter(
          taskBusinessStateTracker -> taskBusinessStateTracker.getParentGeographicLevelName()
              .equals(node)).findFirst();
      first.ifPresent(
          taskBusinessStateTracker -> collectLocationAboveStructureData(message,
              parentsAboveNodeAboveStructure, taskBusinessStateTracker));
    }
  }

  private void collectLocationAboveStructureData(TaskEvent message,
      List<Location> parentsAboveNodeAboveStructure,
      TaskBusinessStateTracker taskBusinessStateTracker) {
    Set<LocationBusinessStateCount> locationBusinessStateObjPerGeoLevel = locationBusinessStatusService.getLocationBusinessStateObjPerGeoLevel(
        message.getAction().getGoal().getPlan().getIdentifier(),
        taskBusinessStateTracker.getParentLocationIdentifier(),
        LocationConstants.STRUCTURE,
        message.getAction().getGoal().getPlan().getLocationHierarchy().getIdentifier());

    Map<String, Long> structureCountsByBusinessStatus = locationBusinessStateObjPerGeoLevel
        .stream()
        .collect(Collectors.groupingBy(LocationBusinessStateCount::getTaskBusinessStatus,
            Collectors.summingLong(LocationBusinessStateCount::getLocationCount)));

    Long countOfAssignedStructuresInArea = planLocationsService.getAssignedChildrenOfLocationBelow(
        message.getAction().getGoal().getPlan().getIdentifier(),
        taskBusinessStateTracker.getParentLocationIdentifier(),
        message.getAction().getGoal().getPlan().getLocationHierarchy().getIdentifier());

    boolean treatedStatus = false;
    boolean visitedStatus = false;
    boolean visitedEffectivelyStatus = false;
    if (message.getAction().getGoal().getPlan().getInterventionType().getName().equals(
        PlanInterventionTypeEnum.IRS.name())) {

      Double notEligible = 0D;
      if (structureCountsByBusinessStatus.containsKey(BusinessStatus.NOT_ELIGIBLE)) {
        notEligible = Double.valueOf(
            structureCountsByBusinessStatus.get(BusinessStatus.NOT_ELIGIBLE));
      }

      Double notVisited = 0D;
      if (structureCountsByBusinessStatus.containsKey(BusinessStatus.NOT_VISITED)) {
        notVisited = Double.valueOf(
            structureCountsByBusinessStatus.get(BusinessStatus.NOT_VISITED));
      }

      Double complete = 0D;
      if (structureCountsByBusinessStatus.containsKey(BusinessStatus.COMPLETE)) {
        complete = Double.valueOf(
            structureCountsByBusinessStatus.get(BusinessStatus.COMPLETE));
      }

      double percentageVisited =
          (countOfAssignedStructuresInArea - notVisited - notEligible) / (
              (double) countOfAssignedStructuresInArea - notEligible) * 100;

      if (percentageVisited > 20d) {
        visitedStatus = true;

        double percentageVisitedEffectively =
            (complete) / ((double) countOfAssignedStructuresInArea - notEligible) * 100;

        if (percentageVisitedEffectively > 85d) {
          visitedEffectivelyStatus = true;
        }
      }
    }
    if (message.getAction().getGoal().getPlan().getInterventionType().getName().equals(
        PlanInterventionTypeEnum.MDA.name())) {
      Double notEligible = 0D;
      if (structureCountsByBusinessStatus.containsKey(BusinessStatus.NOT_ELIGIBLE)) {
        notEligible = Double.valueOf(
            structureCountsByBusinessStatus.get(BusinessStatus.NOT_ELIGIBLE));
      }
      Double notVisited = 0D;
      if (structureCountsByBusinessStatus.containsKey(BusinessStatus.NOT_VISITED)) {
        notVisited = Double.valueOf(
            structureCountsByBusinessStatus.get(BusinessStatus.NOT_VISITED));
      }

      Double smcComplete = 0D;
      if (structureCountsByBusinessStatus.containsKey(BusinessStatus.SMC_COMPLETE)) {
        smcComplete = Double.valueOf(
            structureCountsByBusinessStatus.get(BusinessStatus.SMC_COMPLETE));
      }

      Double spaqComplete = 0D;
      if (structureCountsByBusinessStatus.containsKey(BusinessStatus.SPAQ_COMPLETE)) {
        spaqComplete = Double.valueOf(
            structureCountsByBusinessStatus.get(BusinessStatus.SPAQ_COMPLETE));
      }

      double percentageVisited =
          100 - ((countOfAssignedStructuresInArea - notVisited - notEligible) / (
              countOfAssignedStructuresInArea - notEligible))
              * 100;

      double percentageTreated =
          (smcComplete + spaqComplete) / (((double) countOfAssignedStructuresInArea) - notEligible)
              * 100;

      if (percentageTreated > 95) {
        treatedStatus = true;
      }

      if (percentageVisited > 20) {
        visitedStatus = true;
      }

    }
    updateOrSaveLocationAboveStructureData(parentsAboveNodeAboveStructure,
        taskBusinessStateTracker, treatedStatus,
        visitedStatus,
        visitedEffectivelyStatus);
  }

  private void updateOrSaveLocationAboveStructureData(List<Location> parentsAboveNodeAboveStructure,
      TaskBusinessStateTracker taskBusinessStateTracker, boolean treatedStatus,
      boolean visitedStatus, boolean visitedEffectivelyStatus) {
    Set<LocationAboveStructure> locationAboveStructureCounts = locationBusinessStatusService.getLocationAboveStructureCounts(
        taskBusinessStateTracker.getLocationHierarchyIdentifier(),
        taskBusinessStateTracker.getTaskLocationIdentifier(),
        taskBusinessStateTracker.getPlanIdentifier());

    List<LocationAboveStructure> locationAboveStructureList;
    if (locationAboveStructureCounts.isEmpty()) {

      locationAboveStructureList = parentsAboveNodeAboveStructure.stream()
          .map(location -> getLocationAboveStructure(taskBusinessStateTracker, visitedStatus,
              treatedStatus,
              visitedEffectivelyStatus, location)
          ).collect(Collectors.toList());
    } else {
      locationAboveStructureList = locationAboveStructureCounts.stream().peek(
          locationAboveStructureCount -> {
            locationAboveStructureCount.setVisited(
                visitedStatus);
            locationAboveStructureCount.setVisitedEffectively(
                visitedEffectivelyStatus);
            locationAboveStructureCount.setTreated(treatedStatus);
          }).collect(Collectors.toList());
    }
    locationBusinessStatusService.saveLocationAboveStructureList(
        locationAboveStructureList);
  }

  private LocationAboveStructure getLocationAboveStructure(
      TaskBusinessStateTracker taskBusinessStateTracker,
      boolean finalVisitedStatus, boolean finalTreatedStatus, boolean finalVisitedEffectivelyStatus,
      Location location) {
    return LocationAboveStructure.builder()
        .locationAboveStructureGeographicLevelName(
            taskBusinessStateTracker.getParentGeographicLevelName())
        .locationAboveStructureIdentifier(
            taskBusinessStateTracker.getParentLocationIdentifier())
        .isVisited(finalVisitedStatus)
        .isTreated(finalTreatedStatus)
        .isVisitedEffectively(finalVisitedEffectivelyStatus)
        .locationHierarchyIdentifier(
            taskBusinessStateTracker.getLocationHierarchyIdentifier())
        .parentGeographicLevelName(location.getGeographicLevel().getName())
        .parentLocationName(location.getName())
        .parentLocationIdentifier(location.getIdentifier())
        .planIdentifier(taskBusinessStateTracker.getPlanIdentifier())
        .locationAboveStructureName(
            taskBusinessStateTracker.getParentLocationName())
        .build();
  }

  private TaskBusinessStateTracker getTaskBusinessStateTracker(TaskEvent message,
      String locationGeographicLevelName, String locationName, Location locationAncestor) {
    return TaskBusinessStateTracker.builder()
        .taskBusinessStatus(message.getBusinessStatus())
        .locationHierarchyIdentifier(
            message.getAction().getGoal().getPlan().getLocationHierarchy().getIdentifier())
        .taskBusinessStatusUpdateTime(message.getLastModified())
        .parentGeographicLevelName(locationAncestor.getGeographicLevel().getName())
        .parentLocationIdentifier(locationAncestor.getIdentifier())
        .taskLocationGeographicLevelName(locationGeographicLevelName)
        .parentLocationName(locationAncestor.getName())
        .taskLocationIdentifier(UUID.fromString(message.getLocationId()))
        .taskLocationName(locationName)
        .taskBusinessStatus(message.getBusinessStatus())
        .planIdentifier(message.getAction().getGoal().getPlan().getIdentifier())
        .build();
  }
}
