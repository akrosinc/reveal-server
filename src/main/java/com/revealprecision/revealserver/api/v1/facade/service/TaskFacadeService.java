package com.revealprecision.revealserver.api.v1.facade.service;

import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.api.v1.facade.factory.TaskFacadeFactory;
import com.revealprecision.revealserver.api.v1.facade.models.TaskDto;
import com.revealprecision.revealserver.api.v1.facade.models.TaskFacade;
import com.revealprecision.revealserver.api.v1.facade.models.TaskUpdateFacade;
import com.revealprecision.revealserver.api.v1.facade.util.DateTimeFormatter;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.TaskPriorityEnum;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.TaskEventFactory;
import com.revealprecision.revealserver.messaging.message.Message;
import com.revealprecision.revealserver.messaging.message.TaskAggregate;
import com.revealprecision.revealserver.messaging.message.TaskEvent;
import com.revealprecision.revealserver.messaging.message.TaskLocationPair;
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
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    List<Plan> plans = planIdentifiers.stream().map(UUID::fromString)
        .map(planService::findPlanByIdentifier).collect(Collectors.toList());

    Map<UUID, List<Location>> planTargetsMap = plans.stream().map(
            plan -> getUuidListSimpleEntry(jurisdictionIdentifiers, plan))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
    ReadOnlyKeyValueStore<String, TaskEvent> taskPlanParent = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.taskPlanParent),
            QueryableStoreTypes.keyValueStore()));
    ReadOnlyKeyValueStore<String, TaskAggregate> taskParent = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.taskParent),
            QueryableStoreTypes.keyValueStore()));

    ReadOnlyKeyValueStore<String, TaskEvent> taskStore = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap().get(KafkaConstants.task), QueryableStoreTypes.keyValueStore()));

    log.debug("Before task sync");

    List<TaskFacade> collect = plans.stream().flatMap(plan -> {
      if (plan.getPlanTargetType().getGeographicLevel().getName()
          .equals(LocationConstants.STRUCTURE)) {
        return getTaskFacadeStream(serverVersion, requester, planTargetsMap, taskPlanParent,
            taskParent,
            plan);
      } else {
        return getTaskFacadeStream(serverVersion, requester, planTargetsMap, taskStore, plan);
      }
    }).distinct().collect(Collectors.toList());

    return collect;
  }

  private Stream<TaskFacade> getTaskFacadeStream(Long serverVersion, String requester,
      Map<UUID, List<Location>> planTargetsMap, ReadOnlyKeyValueStore<String, TaskEvent> taskStore,
      Plan plan) {
    return planTargetsMap.get(plan.getIdentifier()).stream()
        .map(location -> plan.getIdentifier() + "_" + location.getIdentifier().toString())
        .map(taskStore::get)
        .filter(taskEvent -> taskEvent.getServerVersion() > serverVersion)
        .map(taskEvent -> TaskFacadeFactory.getTaskFacadeObj(requester,
            taskEvent.getBaseEntityIdentifier().toString(), taskEvent))
        .collect(Collectors.toList()).stream();
  }

  private Stream<TaskFacade> getTaskFacadeStream(Long serverVersion, String requester,
      Map<UUID, List<Location>> planTargetsMap,
      ReadOnlyKeyValueStore<String, TaskEvent> taskPlanParent,
      ReadOnlyKeyValueStore<String, TaskAggregate> taskParent, Plan plan) {
    return planTargetsMap.get(plan.getIdentifier()).stream()
        .peek(planObj -> log.debug("plan Id for task sync: {}", planObj))
        .flatMap(jurisdictionIdentifier -> {
          String taskKey = plan.getIdentifier() + "_" + jurisdictionIdentifier.getIdentifier();
          log.debug("key to retrieve task: {}", taskKey);
          List<TaskLocationPair> taskIds = new ArrayList<>();
          try {
            taskIds = taskParent.get(taskKey).getTaskIds();
          } catch (NullPointerException exp) {
            log.error("key: {} requested is not present in kafka store", taskKey);
          }
          return taskIds.stream()
              .peek(taskIdList -> log.debug("items retrieved from kafka store: {}", taskIdList))
              .filter(taskId -> taskId.getServerVersion() > serverVersion)
              .map(
                  taskId -> taskId.getId() + "_" + plan.getIdentifier() + "_"
                      + jurisdictionIdentifier.getIdentifier()).map(taskPlanParentId -> {
                TaskEvent task = taskPlanParent.get(taskPlanParentId);
                return TaskFacadeFactory.getTaskFacadeObj(requester, taskPlanParentId, task);
              });
        });
  }

  private SimpleEntry<UUID, List<Location>> getUuidListSimpleEntry(
      List<UUID> jurisdictionIdentifiers, Plan plan) {
    List<Location> collect = jurisdictionIdentifiers.stream()
        .map(locationService::findByIdentifier)
        .filter(location -> {
          if (plan.getPlanTargetType().getGeographicLevel().getName()
              .equals(LocationConstants.STRUCTURE)) {
            return location.getGeographicLevel().getName()
                .equals(LocationConstants.OPERATIONAL);
          } else {
            return location.getGeographicLevel().getName()
                .equals(plan.getPlanTargetType().getGeographicLevel().getName());
          }
        }).collect(Collectors.toList());
    return new SimpleEntry<UUID, List<Location>>(plan.getIdentifier(),
        collect);
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
        .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
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
        kafkaTemplate.send(kafkaProperties.getTopicMap().get(KafkaConstants.TASK), taskEvent);

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

  public List<TaskDto> addTaskDtos(List<TaskDto> taskDtos) {
    List<TaskDto> unprocessedTaskIds = new ArrayList<>();

    taskDtos.forEach(taskDto -> {
      try {
        saveTaskDto(taskDto);
      } catch (Exception e) {
        log.error(e.toString());
        e.printStackTrace();
        unprocessedTaskIds.add(taskDto);
      }
    });

    return unprocessedTaskIds;
  }

  private void saveTaskDto(TaskDto taskDto) {

    String taskCode = taskDto.getCode();
    Plan plan = planService.findPlanByIdentifier(UUID.fromString(taskDto.getPlanIdentifier()));
    Action action = actionService.findByTitleAndPlanIdentifier(taskCode, plan.getIdentifier());
    List<LookupTaskStatus> lookupTaskStatuses = taskService.getAllTaskStatus();

    Optional<LookupTaskStatus> taskStatus = lookupTaskStatuses.stream().filter(
            lookupTaskStatus -> lookupTaskStatus.getCode().equalsIgnoreCase(taskDto.getStatus().name()))
        .findFirst();

    Task task = saveTask(taskDto, plan, action, taskStatus);

  }

  private Task saveTask(TaskDto taskDto, Plan plan, Action action,
      Optional<LookupTaskStatus> taskStatus) {
    Task task;
    try {
      task = taskService.getTaskByIdentifier(UUID.fromString(taskDto.getIdentifier()));
    } catch (NotFoundException notFoundException) {
      task = new Task();
      task.setIdentifier(UUID.fromString(taskDto.getIdentifier()));
    }

    LocalDateTime LastModifierFromAndroid = DateTimeFormatter.getLocalDateTimeFromAndroidFacadeString(
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

      task.setEntityStatus(EntityStatus.ACTIVE);

      Location location = null;
      boolean locationFound = true;
      try {
        location = locationService.findByIdentifier(UUID.fromString(taskDto.getStructureId()));
      } catch (NotFoundException notFoundException) {
        locationFound = false;
      }
      if (locationFound) {
        boolean isActionForLocation = ActionUtils.isActionForLocation(action);
        if (isActionForLocation) {
          task.setLocation(location);
        }
      } else {
        //Let's add the new location
        LocationRequest locationRequest = taskDto.getLocationRequest();
        location = locationService.createLocation(locationRequest);
        if (ActionUtils.isActionForLocation(action)) {
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
            List<Location> locationArrayList = new ArrayList<>(locations);
            locationArrayList.add(location);
            person.setLocations(new HashSet<>(locationArrayList));
          } else {
            person.setLocations(Set.of(location));
          }
        }
        task.setPerson(person);
      }
      Task taskSaved = taskService.saveTask(task);
      businessStatusService.setBusinessStatus(taskSaved, taskDto.getBusinessStatus());
      TaskEvent taskEvent = TaskEventFactory.getTaskEventFromTask(taskSaved);
      taskEvent.setOwnerId(UserUtils.getCurrentPrincipleName());
      kafkaTemplate.send(kafkaProperties.getTopicMap().get(KafkaConstants.TASK), taskEvent);
      return taskSaved;
    } else {
      log.error("Unknown task state in sync: {}", taskDto.getStatus().name());
      throw new NotFoundException(Pair.of(Fields.code, taskDto.getStatus().name()),
          LookupTaskStatus.class);
    }
  }
}