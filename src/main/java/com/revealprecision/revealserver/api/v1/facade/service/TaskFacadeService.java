package com.revealprecision.revealserver.api.v1.facade.service;

import static com.revealprecision.revealserver.api.v1.facade.constants.JDBCHelperConstants.LOCATION;
import static com.revealprecision.revealserver.api.v1.facade.constants.JDBCHelperConstants.LOCATION_ENTITY_NAME;
import static com.revealprecision.revealserver.api.v1.facade.constants.JDBCHelperConstants.PERSON;
import static com.revealprecision.revealserver.api.v1.facade.constants.JDBCHelperConstants.PERSON_ENTITY_NAME;

import com.revealprecision.revealserver.api.v1.facade.factory.TaskFacadeFactory;
import com.revealprecision.revealserver.api.v1.facade.models.TaskDto;
import com.revealprecision.revealserver.api.v1.facade.models.TaskFacade;
import com.revealprecision.revealserver.api.v1.facade.models.TaskUpdateFacade;
import com.revealprecision.revealserver.api.v1.facade.properties.TaskFacadeProperties;
import com.revealprecision.revealserver.api.v1.facade.util.DateTimeFormatter;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.TaskPriorityEnum;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import com.revealprecision.revealserver.persistence.domain.LookupTaskStatus;
import com.revealprecision.revealserver.persistence.domain.LookupTaskStatus.Fields;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.service.ActionService;
import com.revealprecision.revealserver.service.GoalService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.PersonService;
import com.revealprecision.revealserver.service.PlanService;
import com.revealprecision.revealserver.service.TaskService;
import com.revealprecision.revealserver.service.UserService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskFacadeService {

  public static final String GENERAL = "GENERAL";
  public static final String DEFAULT_BUSINESS_STATUS = "Not Visited";
  private final MetaDataJdbcService metaDataJdbcService;
  private final UserService userService;
  private final TaskFacadeProperties taskFacadeProperties;
  private final TaskService taskService;
  private final ActionService actionService;
  private final PlanService planService;
  private final PersonService personService;
  private final LocationService locationService;

  public List<TaskFacade> syncTasks(String planIdentifier, List<UUID> jurisdictionIdentifiers) {

    Map<UUID, List<Task>> tasksPerJurisdictionIdentifier = taskService
        .getTasksPerJurisdictionIdentifier(UUID.fromString(planIdentifier),
            jurisdictionIdentifiers);

    List<TaskFacade> taskFacades = tasksPerJurisdictionIdentifier.entrySet().stream().map(entry -> {
      List<Task> tasks = entry.getValue();
      String groupIdentifier = entry.getKey().toString();
      return getTaskFacades(tasks, groupIdentifier);
    }).flatMap(Collection::stream).collect(Collectors.toList());

    return taskFacades;
  }

  private List<TaskFacade> getTaskFacades(List<Task> tasks, String groupIdentifier) {
    return tasks.stream().map(task -> {
      Object businessStatus = getBusinessStatus(task);
      String createdBy = task.getAction().getGoal().getPlan()
          .getCreatedBy(); //TODO: confirm business rule for task creation user(owner)
      User user = getUser(createdBy);
      return TaskFacadeFactory.getEntity(task, (String) businessStatus, user, groupIdentifier);
    }).collect(Collectors.toList());
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

  private Object getBusinessStatus(Task task) {
    String businessStatusField = taskFacadeProperties.getBusinessStatusMapping()
        .get(task.getAction().getGoal().getPlan().getIdentifier().toString());
    if (businessStatusField == null) {
      businessStatusField = taskFacadeProperties.getBusinessStatusMapping().get(GENERAL);
    }

    Object businessStatus = DEFAULT_BUSINESS_STATUS;
    if (task.getLocation() != null) {
      Pair<Class<?>, Object> locationMetadata = metaDataJdbcService.getMetadataFor(LOCATION,
          task.getLocation().getIdentifier()).get(businessStatusField);
      if (locationMetadata != null) {
        if (locationMetadata.getKey() != null) {
          Class<?> aClass = locationMetadata.getKey();
          businessStatus = aClass.cast(locationMetadata.getValue());
        }
      }
    }
    if (task.getPerson() != null) {
      Pair<Class<?>, Object> personMetadata = metaDataJdbcService.getMetadataFor(PERSON,
          task.getPerson().getIdentifier()).get(businessStatusField);
      if (personMetadata != null) {
        if (personMetadata.getKey() != null) {
          Class<?> aClass = personMetadata.getKey();
          businessStatus = aClass.cast(personMetadata.getValue());
        }
      }
    }
    return businessStatus;
  }


  public int setBusinessStatus(Task task, String businessStatus) {
    return metaDataJdbcService.updateMetadata(task.getBaseEntityIdentifier(),
        task.getAction().getLookupEntityType().getTableName(), businessStatus,
        task.getAction().getGoal().getPlan().getIdentifier(), String.class);
  }

  public List<String> updateTaskStatusAndBusinessStatusForListOfTasks(
      List<TaskUpdateFacade> taskUpdateFacades) {
    return taskUpdateFacades.stream().map(this::updateTaskStatusAndBusinessStatus).filter(
        Optional::isPresent).map(Optional::get).map(UUID::toString).collect(Collectors.toList());
  }

  private Optional<UUID> updateTaskStatusAndBusinessStatus(TaskUpdateFacade updateFacade) {

    UUID identifier = null;

    try {
      Task task = taskService.getTaskByIdentifier(
          UUID.fromString(updateFacade.getIdentifier()));

      Optional<LookupTaskStatus> taskStatus = taskService.getAllTaskStatus().stream().filter(
          lookupTaskStatus -> lookupTaskStatus.getCode()
              .equalsIgnoreCase(updateFacade.getStatus())).findFirst();

      if (taskStatus.isPresent()) {
        task.setLookupTaskStatus(taskStatus.get());
        task = taskService.saveTask(task);
        setBusinessStatus(task, updateFacade.getBusinessStatus());
        identifier = task.getIdentifier();
      } else {
        log.error("Unknown task state in task update: {}", updateFacade.getStatus());
      }
    } catch (NotFoundException notFoundException) {
      log.error("Task does not exist: {}", updateFacade.getIdentifier());
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Some error with updating task: {}", e.getMessage());
    }
    return Optional.ofNullable(identifier);
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
        lookupTaskStatus -> lookupTaskStatus.getCode()
            .equalsIgnoreCase(taskDto.getStatus().name())).findFirst();

    LookupEntityType lookupEntityType = action.getLookupEntityType();

    Task task;
    try {
      task = taskService.getTaskByIdentifier(
          UUID.fromString(taskDto.getIdentifier()));
    } catch (NotFoundException notFoundException) {
      task = new Task();
    }

    LocalDateTime LastModifierFromAndroid = DateTimeFormatter
        .getLocalDateTimeFromAndroidFacadeString(
            taskDto.getLastModified());

    if (taskStatus.isPresent()) {

      if (task.getLastModified() != null && LastModifierFromAndroid
          .isAfter(task.getLastModified())) {

        task.setLookupTaskStatus(taskStatus.get());
        task.setAction(action);
        task.setDescription(taskDto.getDescription());
        task.setIdentifier(UUID.fromString(taskDto.getIdentifier()));
        task.setPriority(TaskPriorityEnum.valueOf(taskDto.getPriority().name().toUpperCase()));
        task.setPlan(plan);
        task.setAuthoredOn(DateTimeFormatter.getLocalDateTimeFromAndroidFacadeString(
            taskDto.getAuthoredOn()));
        task.setExecutionPeriodEnd(taskDto.getExecutionPeriod().getEnd() != null
            ? DateTimeFormatter.getLocalDateTimeFromAndroidFacadeString(
            taskDto.getExecutionPeriod().getEnd()).toLocalDate()
            : action.getTimingPeriodEnd());
        task.setExecutionPeriodStart(DateTimeFormatter.getLocalDateTimeFromAndroidFacadeString(
            taskDto.getExecutionPeriod().getStart()).toLocalDate());
        task.setLastModified(LastModifierFromAndroid);

        if (lookupEntityType.getCode().equals(PERSON_ENTITY_NAME)) {
          Person person = personService.getPersonByIdentifier(
              UUID.fromString(taskDto.getForEntity()));
          task.setPerson(person);
        }

        if (lookupEntityType.getCode().equals(LOCATION_ENTITY_NAME)) {
          Location location = locationService.findByIdentifier(
              UUID.fromString(taskDto.getForEntity()));
          task.setLocation(location);
        }

        task.setEntityStatus(EntityStatus.ACTIVE);

        task = taskService.saveTask(task);
        setBusinessStatus(task, taskDto.getBusinessStatus());
        taskService.updateOrganisationsAndLocationsForTask(plan.getIdentifier(), taskStatus.get(),
            task);
      } else {
        log.warn(
            "Ignoring this task from sync as the task submitted is older than the task in the server");
      }
    } else {
      log.error("Unknown task state in sync: {}", taskDto.getStatus().name());
      throw new NotFoundException(
          org.springframework.data.util.Pair.of(Fields.code, taskDto.getStatus().name()),
          LookupTaskStatus.class);
    }

  }
}
