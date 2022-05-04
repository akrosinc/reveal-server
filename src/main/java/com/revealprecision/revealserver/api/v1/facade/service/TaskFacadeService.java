package com.revealprecision.revealserver.api.v1.facade.service;

import com.revealprecision.revealserver.api.v1.facade.factory.TaskFacadeFactory;
import com.revealprecision.revealserver.api.v1.facade.models.Period;
import com.revealprecision.revealserver.api.v1.facade.models.TaskDto;
import com.revealprecision.revealserver.api.v1.facade.models.TaskFacade;
import com.revealprecision.revealserver.api.v1.facade.models.TaskFacade.TaskPriority;
import com.revealprecision.revealserver.api.v1.facade.models.TaskFacade.TaskStatus;
import com.revealprecision.revealserver.api.v1.facade.models.TaskUpdateFacade;
import com.revealprecision.revealserver.api.v1.facade.util.DateTimeFormatter;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.TaskPriorityEnum;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.TaskEventFactory;
import com.revealprecision.revealserver.messaging.TopicConstants;
import com.revealprecision.revealserver.messaging.message.Message;
import com.revealprecision.revealserver.messaging.message.TaskAggregate;
import com.revealprecision.revealserver.messaging.message.TaskEvent;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LookupTaskStatus;
import com.revealprecision.revealserver.persistence.domain.LookupTaskStatus.Fields;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.ActionService;
import com.revealprecision.revealserver.service.BusinessStatusService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.PersonService;
import com.revealprecision.revealserver.service.PlanService;
import com.revealprecision.revealserver.service.TaskService;
import com.revealprecision.revealserver.service.UserService;
import com.revealprecision.revealserver.util.ActionUtils;
import com.revealprecision.revealserver.util.UserUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.data.util.Pair;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskFacadeService {

  private final UserService userService;
  private final TaskService taskService;
  private final ActionService actionService;
  private final PlanService planService;
  private final PersonService personService;
  private final LocationService locationService;
  private final BusinessStatusService businessStatusService;
  private final KafkaTemplate<String, Message> kafkaTemplate;
  private final StreamsBuilderFactoryBean getKafkaStreams;
  private final KafkaProperties kafkaProperties;

  public List<TaskFacade> syncTasks(List<String> planIdentifiers,
      List<UUID> jurisdictionIdentifiers, Long serverVersion, String requester) {

    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, TaskEvent> taskPlanParent = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap().get(TopicConstants.taskPlanParent),
            QueryableStoreTypes.keyValueStore())
    );
    ReadOnlyKeyValueStore<String, TaskAggregate> taskParent = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap().get(TopicConstants.taskParent),
            QueryableStoreTypes.keyValueStore())
    );

    log.debug("Before task sync");

    Set<String> strings = planIdentifiers.stream()
        .flatMap(
            planIdentifier -> jurisdictionIdentifiers.stream().flatMap(
                jurisdictionIdentifier -> taskParent.get(
                        planIdentifier + "_" + jurisdictionIdentifier).getTaskIds().stream()
                    .filter(taskId -> Long.parseLong(taskId.getId().split("\\.")[1]) > serverVersion)
                    .map(taskId -> taskId.getId() + "_" + planIdentifier + "_"
                        + jurisdictionIdentifier))
        ).collect(Collectors.toCollection(LinkedHashSet::new));

    return strings.stream().map(taskPlanParentId -> {
      TaskEvent task = taskPlanParent.get(taskPlanParentId);

      return TaskFacade.builder()
          .code(task.getAction().getTitle())
          .authoredOn(
              DateTimeFormatter.getDateTimeFacadeStringFromLocalDateTime(task.getAuthoredOn()))
          .description(task.getAction().getDescription())
            .executionPeriod(Period
                .between(DateTimeFormatter.getDateTimeFacadeStringFromLocalDateTime(
                        task.getExecutionPeriodStart().atStartOfDay())
                    , DateTimeFormatter.getDateTimeFacadeStringFromLocalDateTime(
                        task.getExecutionPeriodEnd().atStartOfDay())))
          .focus(task.getAction().getIdentifier().toString())
          .forEntity(task.getBaseEntityIdentifier().toString())
          .identifier(task.getIdentifier().toString())
          .planIdentifier(task.getAction().getGoal().getPlan().getIdentifier().toString())
          .priority(TaskPriority.get(task.getPriority().name().toLowerCase()))
          .lastModified(
              DateTimeFormatter.getDateTimeFacadeStringFromLocalDateTime(task.getLastModified()))
          .status(TaskStatus.get(task.getLookupTaskStatus().getCode().toLowerCase()))
          .businessStatus(task.getBusinessStatus())
            .owner(task.getOwner())
            .requester(requester)
          .groupIdentifier(taskPlanParentId.split("_")[2])
          .structureId(task.getBaseEntityIdentifier().toString())
          .serverVersion(task.getServerVersion() == null ? 0 : task.getServerVersion())
          .build();

    }).collect(Collectors.toList());

  }

  private List<TaskFacade> getTaskFacades(Entry<UUID, List<Task>> groupTaskListEntry) {
    List<Task> tasks = groupTaskListEntry.getValue();
    String groupIdentifier = groupTaskListEntry.getKey().toString();
    return tasks.stream().map(task -> getTaskFacade(groupIdentifier, task))
        .collect(Collectors.toList());
  }

  private TaskFacade getTaskFacade(String groupIdentifier, Task task) {
    String businessStatus = businessStatusService.getBusinessStatus(task);
    String createdBy = task.getAction().getGoal().getPlan()
        .getCreatedBy(); //TODO: confirm business rule for task creation user(owner)
    User user = getUser(createdBy);
    log.debug("Creating task facade with task identifier: {}", task.getIdentifier());
    return TaskFacadeFactory.getEntity(task, businessStatus, user, groupIdentifier);
  }

  private User getUser(String createdByUserIdentifier) {
    User user = null;
    try {
      user = userService.getByKeycloakId(UUID.fromString(createdByUserIdentifier));
    } catch (NotFoundException exception) {
      log.debug(String.format("CreatedBy user not found exception: %s", exception.getMessage()));
    }
    return user;
  }


  public List<String> updateTaskStatusAndBusinessStatusForListOfTasks(
      List<TaskUpdateFacade> taskUpdateFacades) {
    return taskUpdateFacades.stream().map(this::updateTaskStatusAndBusinessStatus)
        .filter(Optional::isPresent).map(Optional::get)
        .collect(Collectors.toList());
  }

  private Optional<String> updateTaskStatusAndBusinessStatus(TaskUpdateFacade updateFacade) {
    UUID identifier = null;
    try {
      Task task = taskService.getTaskByIdentifier(UUID.fromString(updateFacade.getIdentifier()));

      Optional<LookupTaskStatus> taskStatus = taskService.getAllTaskStatus().stream().filter(
              lookupTaskStatus -> lookupTaskStatus.getCode().equalsIgnoreCase(updateFacade.getStatus()))
          .findFirst();

      if (taskStatus.isPresent()) {
        task.setLookupTaskStatus(taskStatus.get());
        task.setBusinessStatus(updateFacade.getBusinessStatus());
        businessStatusService.setBusinessStatus(task, updateFacade.getBusinessStatus());
        Task savedTask = taskService.saveTask(task);
        identifier = task.getIdentifier();

        TaskEvent taskEvent = TaskEventFactory.getTaskEventFromTask(savedTask);
        taskEvent.setOwnerId(UserUtils.getCurrentPrincipleName());
        kafkaTemplate.send(kafkaProperties.getTopicMap().get(TopicConstants.TASK), taskEvent);

      } else {
        log.error("Unknown task state in task update: {}", updateFacade.getStatus());
      }
    } catch (NotFoundException notFoundException) {
      log.error("Task does not exist: {}", updateFacade.getIdentifier());
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Some error with updating task: {}", e.getMessage());
    }
    return Optional.ofNullable(identifier.toString());
  }

  public List<TaskDto> addTasks(List<TaskDto> taskDtos) {
    List<TaskDto> unprocessedTaskIds = new ArrayList<>();

    taskDtos.forEach(taskDto -> {
      try {
        saveTask(taskDto);
      } catch (Exception e) {
        log.error(e.toString());
        e.printStackTrace();
        unprocessedTaskIds.add(taskDto);
      }
    });

    return unprocessedTaskIds;
  }

  private void saveTask(TaskDto taskDto) {

    String taskCode = taskDto.getCode();
    Plan plan = planService.getPlanByIdentifier(UUID.fromString(taskDto.getPlanIdentifier()));
    Action action = actionService.findByTitle(taskCode);
    List<LookupTaskStatus> lookupTaskStatuses = taskService.getAllTaskStatus();

    Optional<LookupTaskStatus> taskStatus = lookupTaskStatuses.stream().filter(
            lookupTaskStatus -> lookupTaskStatus.getCode().equalsIgnoreCase(taskDto.getStatus().name()))
        .findFirst();

    Task task;
    try {
      task = taskService.getTaskByIdentifier(UUID.fromString(taskDto.getIdentifier()));
    } catch (NotFoundException notFoundException) {
      task = new Task();
      task.setIdentifier(UUID.fromString(taskDto.getIdentifier()));
    }

    LocalDateTime LastModifierFromAndroid = DateTimeFormatter
        .getLocalDateTimeFromAndroidFacadeString(
            taskDto.getLastModified());

    if (taskStatus.isPresent()) {
      task.setLookupTaskStatus(taskStatus.get());
      task.setAction(action);
      task.setDescription(taskDto.getDescription());
      task.setIdentifier(UUID.fromString(taskDto.getIdentifier()));
      task.setPriority(TaskPriorityEnum.valueOf(taskDto.getPriority().name().toUpperCase()));
      task.setPlan(plan);
      task.setAuthoredOn(
          DateTimeFormatter.getLocalDateTimeFromAndroidFacadeString(taskDto.getAuthoredOn()));
      task.setExecutionPeriodEnd(taskDto.getExecutionPeriod().getEnd() != null
          ? DateTimeFormatter.getLocalDateTimeFromAndroidFacadeString(
          taskDto.getExecutionPeriod().getEnd()).toLocalDate() : action.getTimingPeriodEnd());
      task.setExecutionPeriodStart(DateTimeFormatter.getLocalDateTimeFromAndroidFacadeString(
          taskDto.getExecutionPeriod().getStart()).toLocalDate());
      task.setLastModified(LastModifierFromAndroid);
      task.setBaseEntityIdentifier(UUID.fromString(taskDto.getForEntity()));
      task.setBusinessStatus(taskDto.getBusinessStatus());
      businessStatusService.setBusinessStatus(task, taskDto.getBusinessStatus());
      task.setEntityStatus(EntityStatus.ACTIVE);

      Location location = null;
      boolean locationFound = true;
      try {
        location = locationService.findByIdentifier(
            UUID.fromString(taskDto.getStructureId()));
      } catch (NotFoundException notFoundException) {
        locationFound = false;
      }
      //TODO: We will need to handle the location creation process here for dropped points. Will require android "task add" change.
      if (locationFound) {
        boolean isActionForLocation = ActionUtils.isActionForLocation(action);
        if (isActionForLocation) {
          task.setLocation(location);
        }
      }

      //TODO: Incoming task should have a "client" with metadata instead of person request obj so that we save all client info for subsequent event syncs.
      boolean isActionForPerson = ActionUtils.isActionForPerson(action);
      if (isActionForPerson) {
        Person person;
        try {
          person = personService.getPersonByIdentifier(UUID.fromString(taskDto.getForEntity()));
        } catch (NotFoundException e) {
          //We received task for new Person, record the person
          person = personService.createPerson(taskDto.getPersonRequest());
        }
        if (location != null) {
          Set<Location> locations = person.getLocations();
          if (locations != null) {
            locations.add(location);
            person.setLocations(locations);
          }
        }
        task.setPerson(person);
      }
      Task taskSaved = taskService.saveTask(task);
      TaskEvent taskEvent = TaskEventFactory.getTaskEventFromTask(taskSaved);
      taskEvent.setOwnerId(UserUtils.getCurrentPrincipleName());
      kafkaTemplate.send(kafkaProperties.getTopicMap().get(TopicConstants.TASK), taskEvent);

    } else {
      log.error("Unknown task state in sync: {}", taskDto.getStatus().name());
      throw new NotFoundException(
          Pair.of(Fields.code, taskDto.getStatus().name()),
          LookupTaskStatus.class);
    }

  }
}
