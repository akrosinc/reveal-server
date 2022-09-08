package com.revealprecision.revealserver.api.v1.facade.service;

import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.api.v1.facade.factory.TaskFacadeFactory;
import com.revealprecision.revealserver.api.v1.facade.models.TaskDto;
import com.revealprecision.revealserver.api.v1.facade.models.TaskFacade;
import com.revealprecision.revealserver.api.v1.facade.models.TaskUpdateFacade;
import com.revealprecision.revealserver.api.v1.facade.util.DateTimeFormatter;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.TaskPriorityEnum;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.TaskEventFactory;
import com.revealprecision.revealserver.messaging.message.Message;
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
import com.revealprecision.revealserver.service.MetadataService;
import com.revealprecision.revealserver.service.PersonService;
import com.revealprecision.revealserver.service.PlanService;
import com.revealprecision.revealserver.service.TaskService;
import com.revealprecision.revealserver.service.UserService;
import com.revealprecision.revealserver.util.ActionUtils;
import com.revealprecision.revealserver.util.UserUtils;
import java.time.LocalDateTime;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.util.Pair;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskFacadeService {

  private final TaskService taskService;
  private final ActionService actionService;
  private final PlanService planService;
  private final PersonService personService;
  private final LocationService locationService;
  private final BusinessStatusService businessStatusService;
  private final KafkaTemplate<String, Message> kafkaTemplate;
  private final KafkaProperties kafkaProperties;
  private final MetadataService metadataService;
  private final Environment env;
  private final UserService userService;


  public List<TaskFacade> syncTasks(List<String> planIdentifiers,
      List<UUID> jurisdictionIdentifiers, Long serverVersion, String requester) {

    List<Plan> plans = planIdentifiers.stream().map(UUID::fromString)
        .map(planService::findPlanByIdentifier).collect(Collectors.toList());

    Map<UUID, List<Location>> planTargetsMap = plans.stream().map(
            plan -> getUuidListSimpleEntry(jurisdictionIdentifiers, plan))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    return plans.stream().flatMap(plan -> {
      if (plan.getPlanTargetType().getGeographicLevel().getName()
          .equals(LocationConstants.STRUCTURE)) {
        return taskService.getStructureTaskFacadesByLocationServerVersionAndPlan(
                plan.getIdentifier(),
                planTargetsMap.get(plan.getIdentifier()).stream().map(Location::getIdentifier).collect(
                    Collectors.toList()), serverVersion).stream()
            .filter(task -> task.getTaskFacade() != null)
            .map(task -> {
              TaskFacade taskFacadeObj = TaskFacadeFactory.getTaskFacadeObj(requester,
                  task.getTaskFacade().getParentLocation().toString()
                  , task.getTaskFacade());
              taskFacadeObj.setServerVersion(task.getServerVersion());
              return taskFacadeObj;
            });
      } else {
        return taskService.getNonStructureTaskFacadesByLocationServerVersionAndPlan(
                plan.getIdentifier(),
                planTargetsMap.get(plan.getIdentifier()).stream()
                    .map(Location::getIdentifier)
                    .collect(Collectors.toList()), serverVersion)
            .stream()
            .filter(task -> task.getTaskFacade() != null)
            .map(task -> {
              TaskFacade taskFacadeObj = TaskFacadeFactory.getTaskFacadeObj(requester,
                  task.getTaskFacade().getParentLocation().toString()
                  , task.getTaskFacade());
              taskFacadeObj.setServerVersion(task.getServerVersion());
              return taskFacadeObj;
            });
      }
    }).distinct().collect(Collectors.toList());

  }


  private SimpleEntry<UUID, List<Location>> getUuidListSimpleEntry(
      List<UUID> jurisdictionIdentifiers, Plan plan) {

    Set<Location> locationsWithoutGeoJsonByIdentifierIn = locationService.findLocationsWithoutGeoJsonByIdentifierIn(
        new HashSet<>(jurisdictionIdentifiers));

    List<Location> locations = locationsWithoutGeoJsonByIdentifierIn.stream().filter(
        location -> {
          if (plan.getPlanTargetType().getGeographicLevel().getName()
              .equals(LocationConstants.STRUCTURE)) {
            return
                location.getGeographicLevel().getName().equals(
                    plan.getLocationHierarchy().getNodeOrder().get(
                        plan.getLocationHierarchy().getNodeOrder()
                            .indexOf(LocationConstants.STRUCTURE) - 1));
          } else {
            return location.getGeographicLevel().getName()
                .equals(plan.getPlanTargetType().getGeographicLevel().getName());
          }
        }).collect(Collectors.toList());

    return new SimpleEntry<>(plan.getIdentifier(),
        locations);
  }


  public List<String> updateTaskStatusAndBusinessStatusForListOfTasks(
      List<TaskUpdateFacade> taskUpdateFacades) {

    return taskUpdateFacades.stream().map(taskUpdateFacade -> updateTaskStatusAndBusinessStatus(taskUpdateFacade,
            getOwner()))
        .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
  }

  private String getOwner() {
    String owner = null;

    if (UserUtils.getCurrentPrincipleName()!=null){
      User user = userService.getByKeycloakId(UUID.fromString(UserUtils.getCurrentPrincipleName()));
      owner = user.getUsername();
    }
    return owner;
  }

  private Optional<String> updateTaskStatusAndBusinessStatus(TaskUpdateFacade updateFacade, String owner) {
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
        task.setTaskFacade(TaskEventFactory.getTaskEventFromTask(task));
        Task savedTask = taskService.saveTask(task);
        identifier = task.getIdentifier();

        TaskEvent taskEvent = TaskEventFactory.getTaskEventFromTask(savedTask);
        taskEvent.setOwnerId(UserUtils.getCurrentPrincipleName());
        taskEvent.setOwner(owner);
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
        saveTaskDto(taskDto,getOwner());
      } catch (Exception e) {
        log.error(e.toString(), e);
        e.printStackTrace();
        unprocessedTaskIds.add(taskDto);
      }
    });

    return unprocessedTaskIds;
  }

  private void saveTaskDto(TaskDto taskDto, String owner) throws Exception {

    String taskCode = taskDto.getCode();
    Plan plan = planService.findPlanByIdentifier(UUID.fromString(taskDto.getPlanIdentifier()));
    Action action = actionService.findByTitleAndPlanIdentifier(taskCode, plan.getIdentifier());
    List<LookupTaskStatus> lookupTaskStatuses = taskService.getAllTaskStatus();

    Optional<LookupTaskStatus> taskStatus = lookupTaskStatuses.stream().filter(
            lookupTaskStatus -> lookupTaskStatus.getCode().equalsIgnoreCase(taskDto.getStatus().name()))
        .findFirst();

    Task task = saveTask(taskDto, plan, action, taskStatus,owner);

  }

  private Task saveTask(TaskDto taskDto, Plan plan, Action action,
      Optional<LookupTaskStatus> taskStatus, String owner) throws Exception {
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

          if (Arrays.asList(env.getActiveProfiles()).contains("Simulation")) {
            metadataService.updatePersonDetailsOnElasticSearch(person);
          }
        }
        task.setPerson(person);
      }
      TaskEvent taskEvent = TaskEventFactory.getTaskEventFromTask(task);
      taskEvent.setOwnerId(UserUtils.getCurrentPrincipleName());
      taskEvent.setOwner(owner);
      task.setTaskFacade(taskEvent);
      Task taskSaved = taskService.saveTask(task);
      businessStatusService.setBusinessStatus(taskSaved, taskDto.getBusinessStatus());

      kafkaTemplate.send(kafkaProperties.getTopicMap().get(KafkaConstants.TASK), taskEvent);
      return taskSaved;
    } else {
      log.error("Unknown task state in sync: {}", taskDto.getStatus().name());
      throw new NotFoundException(Pair.of(Fields.code, taskDto.getStatus().name()),
          LookupTaskStatus.class);
    }
  }


}