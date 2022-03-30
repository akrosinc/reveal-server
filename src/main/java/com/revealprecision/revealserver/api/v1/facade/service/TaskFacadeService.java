package com.revealprecision.revealserver.api.v1.facade.service;

import com.revealprecision.revealserver.api.v1.facade.factory.TaskFacadeFactory;
import com.revealprecision.revealserver.api.v1.facade.models.TaskDto;
import com.revealprecision.revealserver.api.v1.facade.models.TaskFacade;
import com.revealprecision.revealserver.api.v1.facade.models.TaskUpdateFacade;
import com.revealprecision.revealserver.api.v1.facade.util.DateTimeFormatter;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.TaskPriorityEnum;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.LookupTaskStatus;
import com.revealprecision.revealserver.persistence.domain.LookupTaskStatus.Fields;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.service.ActionService;
import com.revealprecision.revealserver.service.BusinessStatusService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.PersonService;
import com.revealprecision.revealserver.service.PlanService;
import com.revealprecision.revealserver.service.TaskService;
import com.revealprecision.revealserver.service.UserService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskFacadeService {

  private final UserService userService;
  private final TaskService taskService;
  private final ActionService actionService;
  private final PlanService planService;
  private final BusinessStatusService businessStatusService;

  public List<TaskFacade> syncTasks(List<String> planIdentifiers,
      List<UUID> jurisdictionIdentifiers) {

    return planIdentifiers.stream().map(
            planIdentifier -> taskService.getTasksPerJurisdictionIdentifier(
                    UUID.fromString(planIdentifier), jurisdictionIdentifiers).entrySet().stream()
                .map(this::getTaskFacades).flatMap(Collection::stream).collect(Collectors.toList()))
        .flatMap(Collection::stream).collect(Collectors.toList());
  }

  private List<TaskFacade> getTaskFacades(Entry<UUID, List<Task>> groupTaskListEntry) {
    List<Task> tasks = groupTaskListEntry.getValue();
    String groupIdentifier = groupTaskListEntry.getKey().toString();
    return tasks.stream().map(task -> getTaskFacade(groupIdentifier, task))
        .collect(Collectors.toList());
  }

  private TaskFacade getTaskFacade(String groupIdentifier, Task task) {
    Object businessStatus = businessStatusService.getBusinessStatus(task);
    String createdBy = task.getAction().getGoal().getPlan()
        .getCreatedBy(); //TODO: confirm business rule for task creation user(owner)
    User user = getUser(createdBy);
    return TaskFacadeFactory.getEntity(task, (String) businessStatus, user, groupIdentifier);
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
        .filter(Optional::isPresent).map(Optional::get).map(UUID::toString)
        .collect(Collectors.toList());
  }

  private Optional<UUID> updateTaskStatusAndBusinessStatus(TaskUpdateFacade updateFacade) {

    UUID identifier = null;

    try {
      Task task = taskService.getTaskByIdentifier(UUID.fromString(updateFacade.getIdentifier()));

      Optional<LookupTaskStatus> taskStatus = taskService.getAllTaskStatus().stream().filter(
              lookupTaskStatus -> lookupTaskStatus.getCode().equalsIgnoreCase(updateFacade.getStatus()))
          .findFirst();

      if (taskStatus.isPresent()) {
        task.setLookupTaskStatus(taskStatus.get());
        task = taskService.saveTask(task);
        businessStatusService.setBusinessStatus(task, updateFacade.getBusinessStatus());
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
            lookupTaskStatus -> lookupTaskStatus.getCode().equalsIgnoreCase(taskDto.getStatus().name()))
        .findFirst();

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

      task.setEntityStatus(EntityStatus.ACTIVE);

      task = taskService.saveTask(task);
      businessStatusService.setBusinessStatus(task, taskDto.getBusinessStatus());
      taskService.updateOrganisationsAndLocationsForTask(plan.getIdentifier(), taskStatus.get(),
          task);
    } else {
      log.error("Unknown task state in sync: {}", taskDto.getStatus().name());
      throw new NotFoundException(
          org.springframework.data.util.Pair.of(Fields.code, taskDto.getStatus().name()),
          LookupTaskStatus.class);
    }

  }
}
