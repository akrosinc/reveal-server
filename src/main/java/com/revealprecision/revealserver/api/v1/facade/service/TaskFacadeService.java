package com.revealprecision.revealserver.api.v1.facade.service;

import static com.revealprecision.revealserver.api.v1.facade.constants.JDBCHelperConstants.LOCATION;
import static com.revealprecision.revealserver.api.v1.facade.constants.JDBCHelperConstants.LOCATION_ENTITY_NAME;
import static com.revealprecision.revealserver.api.v1.facade.constants.JDBCHelperConstants.PERSON;
import static com.revealprecision.revealserver.api.v1.facade.constants.JDBCHelperConstants.PERSON_ENTITY_NAME;

import com.revealprecision.revealserver.api.v1.facade.factory.TaskFacadeFactory;
import com.revealprecision.revealserver.api.v1.facade.models.TaskDto;
import com.revealprecision.revealserver.api.v1.facade.models.TaskFacade;
import com.revealprecision.revealserver.api.v1.facade.properties.TaskFacadeProperties;
import com.revealprecision.revealserver.api.v1.facade.util.DateTimeFormatter;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import com.revealprecision.revealserver.persistence.domain.LookupTaskStatus;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.service.ActionService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.PersonService;
import com.revealprecision.revealserver.service.PlanService;
import com.revealprecision.revealserver.service.TaskService;
import com.revealprecision.revealserver.service.UserService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TaskFacadeService {

  public static final String GENERAL = "GENERAL";
  private final MetaDataJdbcService metaDataJdbcService;

  private final UserService userService;

  private final TaskJDBCService taskJDBCService;

  private final TaskFacadeProperties taskFacadeProperties;

  private final TaskService taskService;

  private final ActionService actionService;

  private final PlanService planService;

  private final PersonService personService;

  private final LocationService locationService;

  @Autowired
  public TaskFacadeService(MetaDataJdbcService metaDataJdbcService, UserService userService,
      TaskJDBCService taskJDBCService, TaskFacadeProperties taskFacadeProperties,
      TaskService taskService, ActionService actionService, PlanService planService,
      PersonService personService, LocationService locationService) {
    this.metaDataJdbcService = metaDataJdbcService;
    this.userService = userService;
    this.taskJDBCService = taskJDBCService;
    this.taskFacadeProperties = taskFacadeProperties;
    this.taskService = taskService;
    this.actionService = actionService;
    this.planService = planService;
    this.personService = personService;
    this.locationService = locationService;
  }

  public List<TaskFacade> syncTasks(String plan, String group) {
    return taskJDBCService.getTasksByPlanAndJurisdictionList(UUID.fromString(plan),
            Arrays.asList(group.split(",")))
        .stream().map(task -> {
          Object businessStatus = getBusinessStatus(task);
          String createdBy = task.getAction().getGoal().getPlan().getCreatedBy();
          User user = userService.getByIdentifier(UUID.fromString(createdBy));

          return TaskFacadeFactory.getEntity(task, (String) businessStatus, user.getUsername(),
              group);

        }).collect(Collectors.toList());
  }

  private Object getBusinessStatus(Task task) {
    String businessStatusField = taskFacadeProperties.getBusinessStatusMapping()
        .get(task.getAction().getGoal().getPlan().getIdentifier().toString());
    if (businessStatusField == null) {
      businessStatusField = taskFacadeProperties.getBusinessStatusMapping().get(GENERAL);
    }

    Object businessStatus = null;
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


  public void setbusinessStatus(Task task, String businessStatus) {
    metaDataJdbcService.updateMetadata(task.getBaseEntityIdentifier(),
        task.getAction().getLookupEntityType().getTableName(), businessStatus,
        task.getAction().getGoal().getPlan().getIdentifier(), String.class);
  }

  public List<TaskDto> addTasks(List<TaskDto> taskDtos) {
    List<TaskDto> unprocessedTaskIds = new ArrayList<>();

    taskDtos.forEach(taskDto -> {
      Action action = actionService.getByIdentifier(UUID.fromString(taskDto.getFocus()));
      Plan plan = planService.getPlanByIdentifier(UUID.fromString(taskDto.getPlanIdentifier()));
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

      try {
        if (taskStatus.isPresent()) {

          task.setLookupTaskStatus(taskStatus.get());
          task.setAction(action);
          task.setDescription(taskDto.getDescription());
          task.setIdentifier(UUID.fromString(taskDto.getIdentifier()));
          task.setPlan(plan);
          task.setAuthoredOn(DateTimeFormatter.getLocalDateTimeFromAndroidFacadeString(
              taskDto.getAuthoredOn()));
          task.setExecutionPeriodEnd(taskDto.getExecutionPeriod().getEnd() != null
              ? DateTimeFormatter.getLocalDateTimeFromAndroidFacadeString(
              taskDto.getExecutionPeriod().getEnd()).toLocalDate()
              : action.getTimingPeriodEnd());
          task.setExecutionPeriodStart(DateTimeFormatter.getLocalDateTimeFromAndroidFacadeString(
              taskDto.getExecutionPeriod().getStart()).toLocalDate());
          task.setLastModified(DateTimeFormatter.getLocalDateTimeFromAndroidFacadeString(
              taskDto.getLastModified()));

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

          taskService.saveTask(task);
          setbusinessStatus(task, taskDto.getBusinessStatus());
          taskService.updateOrganisationsAndLocationsForTask(plan.getIdentifier(), taskStatus.get(),
              task);
        } else {
          log.error("Unknown task state in sync: {}", taskDto.getStatus().name());
          unprocessedTaskIds.add(taskDto);
        }
      } catch (NotFoundException notFoundException) {
        unprocessedTaskIds.add(taskDto);
        log.error(notFoundException.toString());
      }
    });

    return unprocessedTaskIds;
  }
}
