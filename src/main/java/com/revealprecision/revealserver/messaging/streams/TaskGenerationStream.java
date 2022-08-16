package com.revealprecision.revealserver.messaging.streams;

import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.TaskEventFactory;
import com.revealprecision.revealserver.messaging.message.TaskAggregate;
import com.revealprecision.revealserver.messaging.message.TaskEvent;
import com.revealprecision.revealserver.messaging.message.TaskLocationPair;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.UserService;
import com.revealprecision.revealserver.util.ActionUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("Reveal-Streams")
public class TaskGenerationStream {

  private final KafkaProperties kafkaProperties;
  private final UserService userService;
  private final LocationRelationshipService locationRelationshipService;
  private final LocationService locationService;
  private final Logger taskLog = LoggerFactory.getLogger("stream-file");


  @Bean
  public KStream<String, TaskEvent> startTaskGeneration(StreamsBuilder streamsBuilder) {

    //TODO: fix comments
    //get Input Tasks
    KStream<String, TaskEvent> taskStream = streamsBuilder
        .stream(kafkaProperties.getTopicMap().get(KafkaConstants.TASK),
            Consumed.with(Serdes.String(), new JsonSerde<>(TaskEvent.class)))
        .filter((k, taskEvent) -> taskEvent.getAction().getGoal().getPlan().getPlanTargetTypeEvent()
            .getGeographicLevelName().equals(
                LocationConstants.STRUCTURE))
        .mapValues(((k, taskEvent) -> {
          if (taskEvent.getOwnerId() != null) {
            try {
              User user = userService.getByKeycloakId(UUID.fromString(taskEvent.getOwnerId()));
              taskEvent.setOwner(user.getUsername());
            } catch (NotFoundException e) {
              log.error("error getting user: {}", e.getMessage());
            }
          }
          return taskEvent;
        }))
        .selectKey((k, taskEvent) -> taskEvent.getBaseEntityIdentifier().toString());

    taskStream.peek((k, v) -> taskLog.debug("taskStream k: {}, v: {}", k, v));

    //split task stream into personTask stream, locationTask stream and otherTask stream
    Map<String, KStream<String, TaskEvent>> splitStreamMap = taskStream
        .split(Named.as("split"))
        .branch((k, taskEvent) -> taskEvent.getPersonId() != null, Branched.as("PersonStream"))
        .branch((k, taskEvent) -> taskEvent.getLocationId() != null, Branched.as("LocationStream"))
        .defaultBranch(Branched.as("OtherStream"));

    // for each person task get that persons locations and create a task event for each location
    KStream<String, TaskEvent> personStream = splitStreamMap.get("splitPersonStream")
        .flatMapValues((k, personTask) -> duplicatePersonTasksPerPersonsLocation(personTask))
        .selectKey((k, taskEvent) -> taskEvent.getPersonLocationId().toString());

    personStream.peek((k, v) -> taskLog.debug("personStream k: {}, v: {}", k, v));

    // merge all streams back together (now that person locations are known)
    KStream<String, TaskEvent> mergedStream =
        personStream
            .merge(splitStreamMap.get("splitLocationStream"))
            .merge(splitStreamMap.get("splitOtherStream"));

    mergedStream.peek((k, v) -> taskLog.debug("mergedStream k: {}, v: {}", k, v));

    KStream<String, TaskEvent> stringTaskEventKStream1 = mergedStream.mapValues((k, taskEvent) -> {

      Location locationParent = locationService.getLocationParentByLocationIdentifierAndHierarchyIdentifier(
          taskEvent.getPersonLocationId() != null ? taskEvent.getPersonLocationId()
              : taskEvent.getBaseEntityIdentifier(),
          taskEvent.getAction().getGoal().getPlan().getLocationHierarchy().getIdentifier());

      //TODO: this should change in the event the android task sync request changes to request items by specifying items above the operational area
      //This is updated to make the task aggregation a little bit leaner
      taskEvent.setAncestors(List.of(locationParent.getIdentifier()));
      return taskEvent;
    });

    stringTaskEventKStream1.peek(
        (k, v) -> taskLog.debug("stringTaskEventKStream1 k: {}, v: {}", k, v));
    // for each ancestor in the task event list create a task event
    // key each record with task + "_" + plan + "_" + ancestor
    KStream<String, TaskEvent> stringTaskEventKStream = stringTaskEventKStream1
        .flatMapValues((k, locationTask) -> duplicateTaskEventPerAncestor(locationTask))
        .selectKey((k, taskEvent) -> createTaskPlanAncestorKey(taskEvent));

    stringTaskEventKStream.peek(
        (k, v) -> taskLog.debug("stringTaskEventKStream k: {}, v: {}", k, v));

    // create a table of the records for task Events
    // keyed on task + "_" + plan + "_" + ancestor
    stringTaskEventKStream
        .repartition()
        .toTable(Materialized.<String, TaskEvent, KeyValueStore<Bytes, byte[]>>
                as(kafkaProperties.getStoreMap().get(KafkaConstants.taskPlanParent))
            .withValueSerde(new JsonSerde<>(TaskEvent.class))
            .withKeySerde(Serdes.String()));

    // ignore records where the key does not have format task + "_" + plan + "_" + ancestor
    KStream<String, TaskEvent> stringStringKStream = stringTaskEventKStream
        .filter((k, v) -> k.split("_").length > 2);

//    stringStringKStream.print(Printed.<String, TaskEvent>toSysOut());
    // Group the records by plan and ancestor
    KGroupedStream<String, TaskEvent> stringStringKGroupedStream = stringStringKStream
        .selectKey((k, v) -> createPlanAncestorKey(k))
        .groupByKey(Grouped.with(Serdes.String(), new JsonSerde<>(TaskEvent.class)));

    // Accumulate task events per Plan and ancestor
    // the table will return a list of taskIds given plan and location ancestor
    // using the taskIds retrieve the applicable tasks from the taskParentPlanTable above by
    // combining the taskId with plan and location ancestor
    // i.e taskId + "_" + planId + "_" + ancestorId;
    stringStringKGroupedStream
        .aggregate(TaskAggregate::new,
            (k, taskEvent, aggregate) -> collectOrRemoveTaskEventsInTaskAggregateObject(taskEvent,
                aggregate),
            Materialized.<String, TaskAggregate, KeyValueStore<Bytes, byte[]>>as(
                    kafkaProperties.getStoreMap().get(KafkaConstants.taskParent))
                .withKeySerde(Serdes.String())
                .withValueSerde(new JsonSerde<>(TaskAggregate.class)));

    return taskStream;
  }

  @Bean
  public KStream<String, TaskEvent> startTaskGenerationLite(StreamsBuilder streamsBuilder) {

    //TODO: fix comments
    //get Input Tasks
    KStream<String, TaskEvent> taskStream = streamsBuilder
        .stream(kafkaProperties.getTopicMap().get(KafkaConstants.TASK),
            Consumed.with(Serdes.String(), new JsonSerde<>(TaskEvent.class)))
        .filter(
            (k, taskEvent) -> !taskEvent.getAction().getGoal().getPlan().getPlanTargetTypeEvent()
                .getGeographicLevelName().equals(
                    LocationConstants.STRUCTURE))
        .flatMapValues(((k, taskEvent) -> {
          List<TaskEvent> taskEvents = new ArrayList<>();
          if (taskEvent.getOwnerId() != null) {
            try {
              User user = userService.getByKeycloakId(UUID.fromString(taskEvent.getOwnerId()));
              taskEvent.setOwner(user.getUsername());
            } catch (NotFoundException e) {
              log.error("error getting user: {}", e.getMessage());
            }
          }

          UUID groupId = null;

          if (ActionUtils.isActionForLocation(taskEvent.getAction())) {
            Location locationParent = locationService.getLocationParentByLocationIdentifierAndHierarchyIdentifier(
                taskEvent.getBaseEntityIdentifier(),
                taskEvent.getAction().getGoal().getPlan().getLocationHierarchy().getIdentifier());

            if (locationParent != null) {
              groupId = locationParent.getIdentifier();
            }
            taskEvent.setParentLocation(groupId);
            taskEvents.add(taskEvent);
          }
          if (ActionUtils.isActionForPerson(taskEvent.getAction())) {
            List<TaskEvent> taskEventObjs = duplicatePersonTasksPerPersonsLocation(taskEvent);
            taskEvents.addAll(taskEventObjs);
          }

          return taskEvents;
        }))
        .selectKey((k, taskEvent) -> {

          String locationId = "";
          if (ActionUtils.isActionForLocation(taskEvent.getAction())) {
            locationId = taskEvent.getBaseEntityIdentifier().toString();
          }
          if (ActionUtils.isActionForPerson(taskEvent.getAction())) {
            locationId = taskEvent.getPersonLocationId().toString();
          }

          return taskEvent.getAction().getGoal().getPlan().getIdentifier() + "_" +
              locationId;
        });

    taskStream.peek((k, v) -> taskLog.debug("taskStreamLite k: {}, v: {}", k, v));

    taskStream.repartition().toTable(Materialized.<String, TaskEvent, KeyValueStore<Bytes, byte[]>>
            as(kafkaProperties.getStoreMap().get(KafkaConstants.task))
        .withValueSerde(new JsonSerde<>(TaskEvent.class))
        .withKeySerde(Serdes.String()));

    return taskStream;
  }

  private String createPlanAncestorKey(String k) {
    return k.split("_")[1] + "_" + k.split("_")[2];
  }

  private TaskAggregate collectOrRemoveTaskEventsInTaskAggregateObject(TaskEvent taskEvent,
      TaskAggregate aggregate) {

    aggregate.setTaskIds(
        TaskAggregate.removeAllExcept(aggregate.getTaskIds(),
            taskEvent.getIdentifier().toString()));
    aggregate.getTaskIds().add(
        new TaskLocationPair(taskEvent.getLocationName(),
            taskEvent.getIdentifier().toString(), taskEvent.getServerVersion()));
    return aggregate;
  }

  private List<TaskEvent> duplicateTaskEventPerAncestor(TaskEvent locationTask) {
    return locationTask.getAncestors().stream().map(
            ancestor -> addAncestorToTaskEvent(locationTask, ancestor))
        .collect(Collectors.toList());
  }

  private String createTaskPlanAncestorKey(TaskEvent v) {
    return v.getIdentifier().toString().concat("_")
        .concat(v.getAction().getGoal().getPlan().getIdentifier().toString()).concat("_")
        .concat(v.getFlattenedLocationAncestor().toString());
  }

  private TaskEvent addAncestorToTaskEvent(TaskEvent locationTask,
      UUID ancestor) {
    TaskEvent taskEvent = TaskEventFactory.copyTaskEvent(locationTask);
    taskEvent.setFlattenedLocationAncestor(ancestor);
    return taskEvent;
  }


  @Transactional
  List<TaskEvent> duplicatePersonTasksPerPersonsLocation(TaskEvent personTask) {
    return locationService.getLocationsByPeople(UUID.fromString(personTask.getPersonId())).stream()
        .map(personLocation -> getPersonTaskEventWithLocation(personTask, personLocation))
        .collect(Collectors.toList());
  }

  private TaskEvent getPersonTaskEventWithLocation(TaskEvent personTask, Location personLocation) {
    TaskEvent taskEvent = TaskEventFactory.copyTaskEvent(personTask);
    taskEvent.setPersonLocationId(personLocation.getIdentifier());

    Location locationParent = locationService.getLocationParentByLocationIdentifierAndHierarchyIdentifier(
        personLocation.getIdentifier(),
        taskEvent.getAction().getGoal().getPlan().getLocationHierarchy().getIdentifier());
    taskEvent.setParentLocation(locationParent.getIdentifier());

    return taskEvent;
  }

}
