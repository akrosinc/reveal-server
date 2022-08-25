package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.messaging.message.TaskEvent;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.domain.TaskBusinessStateTracker;
import com.revealprecision.revealserver.persistence.repository.TaskBusinessStateTrackerRepository;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.LocationService;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskListener extends Listener {

  private final TaskBusinessStateTrackerRepository taskBusinessStateTrackerRepository;
  private final LocationRelationshipService locationRelationshipService;
  private final LocationService locationService;

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('TASK')}", groupId = "reveal_server_group")
  public void listenGroupFoo(TaskEvent message) throws IOException {
    //TODO: make this traceable - i.e. the application should know when task generation starts / ends
    log.info("Received Message in group foo: {}", message.toString());
    init();

    List<TaskBusinessStateTracker> taskBusinessStateTrackers = taskBusinessStateTrackerRepository.findFirstByLocationHierarchyIdentifierAndTaskLocationIdentifierAndPlanIdentifier(
        message.getAction().getGoal().getPlan().getLocationHierarchy().getIdentifier(),
        UUID.fromString(message.getLocationId()),
        message.getAction().getGoal().getPlan().getIdentifier());

    if (!taskBusinessStateTrackers.isEmpty()) {
      List<TaskBusinessStateTracker> trackers = taskBusinessStateTrackers.stream()
          .peek(taskBusinessStateTracker1 -> {
            if (message.getLastModified()
                .isAfter(taskBusinessStateTracker1.getTaskBusinessStatusUpdateTime())) {
              taskBusinessStateTracker1.setTaskBusinessStatus(message.getBusinessStatus());
              taskBusinessStateTracker1.setTaskBusinessStatusUpdateTime(
                  message.getLastModified());
            }
          }).collect(Collectors.toList());

      taskBusinessStateTrackerRepository.saveAll(trackers);

    } else {

      LocationRelationship locationRelationshipsForLocation = locationRelationshipService.getLocationRelationshipsForLocation(
          message.getAction().getGoal().getPlan().getLocationHierarchy().getIdentifier(),
          UUID.fromString(message.getLocationId()));

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

      List<TaskBusinessStateTracker> trackers = locationRelationshipsForLocation.getAncestry()
          .stream().map(ancestor -> {

            Location locationAncestor = locationService.findByIdentifier(ancestor);

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
          }).collect(Collectors.toList());

      taskBusinessStateTrackerRepository.saveAll(trackers);

    }
  }
}
