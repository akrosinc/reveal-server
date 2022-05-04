package com.revealprecision.revealserver.messaging.streams;

import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.TaskEventFactory;
import com.revealprecision.revealserver.messaging.TopicConstants;
import com.revealprecision.revealserver.messaging.message.LocationAncestor;
import com.revealprecision.revealserver.messaging.message.LocationRelationshipMessage;
import com.revealprecision.revealserver.messaging.message.TaskAggregate;
import com.revealprecision.revealserver.messaging.message.TaskEvent;
import com.revealprecision.revealserver.messaging.message.TaskLocationPair;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.repository.TaskRepository;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.BusinessStatusService;
import com.revealprecision.revealserver.service.PersonService;
import com.revealprecision.revealserver.service.UserService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TaskGenerationStream {

  private final TaskRepository taskRepository;
  private final BusinessStatusService businessStatusService;
  private final PersonService personService;
  private final KafkaProperties kafkaProperties;
  private final UserService userService;


  @Bean
  public KStream<String, TaskEvent> startTaskGeneration(StreamsBuilder streamsBuilder) {
    //get Input Tasks
    KStream<String, TaskEvent> taskStream = streamsBuilder
        .stream(kafkaProperties.getTopicMap().get(TopicConstants.TASK),
            Consumed.with(Serdes.String(), new JsonSerde<>(TaskEvent.class)))
        .mapValues(((k, taskEvent) -> {
          if (taskEvent.getOwnerId()!=null) {
            try {
              User user = userService.getByKeycloakId(UUID.fromString(taskEvent.getOwnerId()));
              taskEvent.setOwner(user.getUsername());
            }catch (NotFoundException e){
              log.error("error getting user: {}",e.getMessage());
            }
          }
          return taskEvent;
        }))
        .selectKey((k, taskEvent) -> taskEvent.getBaseEntityIdentifier().toString());

    //get Structure Table (with ancestry List)
    KTable<String, LocationRelationshipMessage> structuresWithAncestry = streamsBuilder
        .table(kafkaProperties.getTopicMap().get(TopicConstants.STRUCTURES_IMPORTED_FOR_TASK),
            Consumed.with(Serdes.String(), new JsonSerde<>(LocationRelationshipMessage.class)));

    //split task stream into personTask stream, locationTask stream and otherTask stream
    Map<String, KStream<String, TaskEvent>> splitStreamMap = taskStream
        .split(Named.as("split"))
        .branch((k, taskEvent) -> taskEvent.getPersonId() != null, Branched.as("PersonStream"))
        .branch((k, taskEvent) -> taskEvent.getLocationId() != null, Branched.as("LocationStream"))
        .defaultBranch(Branched.as("OtherStream"));

    // for each person task get that persons locations and create a task event for each location
    KStream<String, TaskEvent> personStream = splitStreamMap.get("splitPersonStream")
        .flatMapValues((k, personTask) -> duplicatePersonTasksPerPersonsLocation(personTask))
        .selectKey((k, taskEvent) -> taskEvent.getBaseEntityIdentifier().toString());

    // merge all streams back together (now that person locations are known)
    KStream<String, TaskEvent> mergedStream =
        personStream
            .merge(splitStreamMap.get("splitLocationStream"))
            .merge(splitStreamMap.get("splitOtherStream"));

    //join the task (with their locations) to the structure table so that
    // the ancestry records can be added to the task event
    KStream<String, TaskEvent> taskAncestryParentPlanStream = mergedStream
        .join(structuresWithAncestry, this::addLocationAncestorsToTaskEventFromJoinedObjects);

    // get the business status for the task (i.e. its base entity) and attach to the task event
    // key each record with task + "_" + plan
    KStream<String, TaskEvent> taskAncestryParentPlanStreamWithBusinessStatus = taskAncestryParentPlanStream
        .mapValues((k, taskEvent) -> addBusinessStatusToTaskEvent(taskEvent))
        .selectKey((k, taskEvent) -> getTaskPlanKey(taskEvent));
    taskAncestryParentPlanStreamWithBusinessStatus.to(
        kafkaProperties.getTopicMap().get(TopicConstants.TASK_PLAN));

    // for each ancestor in the task event list create a task event
    // key each record with task + "_" + plan + "_" + ancestor
    KStream<String, TaskEvent> stringTaskEventKStream = taskAncestryParentPlanStreamWithBusinessStatus
        .flatMapValues((k, locationTask) -> duplicateTaskEventPerAncestor(locationTask))
        .selectKey((k, taskEvent) -> createTaskPlanAncestorKey(taskEvent));
    stringTaskEventKStream.to(kafkaProperties.getTopicMap().get(TopicConstants.TASK_PARENT_PLAN));

    // create a table of the records for task Events
    // keyed on task + "_" + plan + "_" + ancestor
    KTable<String, TaskEvent> taskParentPlanTable = streamsBuilder.table(
        kafkaProperties.getTopicMap().get(TopicConstants.TASK_PARENT_PLAN),
        Consumed.with(Serdes.String(), new JsonSerde<>(TaskEvent.class)),
        Materialized.as(kafkaProperties.getStoreMap().get(TopicConstants.taskPlanParent)));

    // ignore records where the key does not have format task + "_" + plan + "_" + ancestor
    KStream<String, TaskEvent> stringStringKStream = stringTaskEventKStream
        .filter((k, v) -> k.split("_").length > 2);

    // Group the records by plan and ancestor
    KGroupedStream<String, TaskEvent> stringStringKGroupedStream = stringStringKStream
        .selectKey((k, v) -> createPlanAncestorKey(k))
        .groupByKey(Grouped.with(Serdes.String(), new JsonSerde<>(TaskEvent.class)));

    // Accumulate task events per Plan and ancestor
    // the table will return a list of taskIds given plan and location ancestor
    // using the taskIds retrieve the applicable tasks from the taskParentPlanTable above by
    // combining the taskId with plan and location ancestor
    // i.e taskId + "_" + planId + "_" + ancestorId;
    //
    // the aggregation will check the task state and if its CANCELLED then it will be removed from the table
    KTable<String, TaskAggregate> aggregatedTaskStream = stringStringKGroupedStream
        .aggregate(TaskAggregate::new,
            (k, taskEvent, aggregate) -> collectOrRemoveTaskEventsInTaskAggregateObject(taskEvent,
                aggregate),
            Materialized.<String, TaskAggregate, KeyValueStore<Bytes, byte[]>>as(
                    kafkaProperties.getStoreMap().get(TopicConstants.taskParent))
                .withKeySerde(Serdes.String())
                .withValueSerde(new JsonSerde<>(TaskAggregate.class)));
    return taskStream;
  }

  private String getTaskPlanKey(TaskEvent v) {
    return v.getIdentifier().toString()
        .concat("_")
        .concat(v.getAction().getGoal().getPlan().getIdentifier().toString());
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
              taskEvent.getIdentifier().toString(),taskEvent.getServerVersion()));
    return aggregate;
  }

  private List<TaskEvent> duplicateTaskEventPerAncestor(TaskEvent locationTask) {
    return locationTask.getLocationAncestors().stream().map(
            locationAncestor -> addAncestorToTaskEvent(locationTask, locationAncestor))
        .collect(Collectors.toList());
  }

  private String createTaskPlanAncestorKey(TaskEvent v) {
    return v.getIdentifier().toString().concat("_")
        .concat(v.getAction().getGoal().getPlan().getIdentifier().toString()).concat("_")
        .concat(v.getFlattenedLocationAncestor().getLocationId());
  }

  private TaskEvent addAncestorToTaskEvent(TaskEvent locationTask,
      LocationAncestor locationAncestor) {
    TaskEvent taskEvent = TaskEventFactory.copyTaskEvent(locationTask);
    taskEvent.setFlattenedLocationAncestor(locationAncestor);
    return taskEvent;
  }

  private TaskEvent addBusinessStatusToTaskEvent(TaskEvent taskEvent) {
    taskRepository.findById(taskEvent.getIdentifier()).ifPresent(
        value -> taskEvent.setBusinessStatus(businessStatusService.getBusinessStatus(value)));
    return taskEvent;
  }

  private TaskEvent addLocationAncestorsToTaskEventFromJoinedObjects(TaskEvent taskEvent,
      LocationRelationshipMessage locationRelationshipMessage) {
    taskEvent.setLocationAncestors(locationRelationshipMessage.getLocationAncestors());
    taskEvent.setLocationName(locationRelationshipMessage.getLocationName());
    return taskEvent;
  }

  private List<TaskEvent> duplicatePersonTasksPerPersonsLocation(TaskEvent personTask) {
    Person person = personService.getPersonByIdentifier(UUID.fromString(personTask.getPersonId()));
    return person.getLocations().stream()
        .map(personLocation -> getPersonTaskEventWithLocation(personTask, personLocation))
        .collect(Collectors.toList());
  }

  private TaskEvent getPersonTaskEventWithLocation(TaskEvent personTask, Location personLocation) {
    TaskEvent taskEvent = TaskEventFactory.copyTaskEvent(personTask);
    taskEvent.setBaseEntityIdentifier(personLocation.getIdentifier());
    return taskEvent;
  }


}
