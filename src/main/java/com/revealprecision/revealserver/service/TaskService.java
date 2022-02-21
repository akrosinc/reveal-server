package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.TaskEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.TaskCreateRequest;
import com.revealprecision.revealserver.api.v1.dto.request.TaskUpdateRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.PlanStatusEnum;
import com.revealprecision.revealserver.enums.TaskPriorityEnum;
import com.revealprecision.revealserver.exceptions.DuplicateTaskCreationException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.QueryGenerationException;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LookupTaskStatus;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.domain.Task.Fields;
import com.revealprecision.revealserver.persistence.domain.actioncondition.Query;
import com.revealprecision.revealserver.persistence.repository.LookupTaskStatusRepository;
import com.revealprecision.revealserver.persistence.repository.TaskRepository;
import com.revealprecision.revealserver.persistence.specification.TaskSpec;
import com.revealprecision.revealserver.service.models.TaskSearchCriteria;
import com.revealprecision.revealserver.util.ConditionQueryUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TaskService {

  private static final String LOCATION = "Location";
  private static final String PERSON = "Person";
  private final TaskRepository taskRepository;
  private final PlanService planService;
  private final ActionService actionService;
  private final PersonService personService;
  private final LocationService locationService;
  private final LookupTaskStatusRepository lookupTaskStatusRepository;
  private final EntityFilterService entityFilterService;

  @Autowired
  public TaskService(TaskRepository taskRepository, FormService formService,
      PlanService planService,
      ActionService actionService, LocationService locationService,
      LookupTaskStatusRepository lookupTaskStatusRepository, PersonService personService,
      JdbcTemplate jdbcTemplate, EntityFilterService entityFilterService) {
    this.taskRepository = taskRepository;
    this.planService = planService;
    this.actionService = actionService;
    this.locationService = locationService;
    this.lookupTaskStatusRepository = lookupTaskStatusRepository;
    this.personService = personService;
    this.entityFilterService = entityFilterService;
  }

  public Page<Task> searchTasks(TaskSearchCriteria taskSearchCriteria, Pageable pageable) {
    return taskRepository.findAll(TaskSpec.getTaskSpecification(taskSearchCriteria), pageable);
  }

  public Long countTasksBySearchCriteria(TaskSearchCriteria taskSearchCriteria) {
    return taskRepository.count(TaskSpec.getTaskSpecification(taskSearchCriteria));
  }

  public Page<Task> getTasks(Pageable pageable) {
    return taskRepository.findAll(pageable);
  }

  public Long getAllTaskCount() {
    return taskRepository.count();
  }

  public Task createTask(TaskCreateRequest taskRequest) {

    Action action = actionService.getByIdentifier(taskRequest.getActionIdentifier());

    LookupTaskStatus lookupTaskStatus = lookupTaskStatusRepository.getById(
        taskRequest.getLookupTaskStatusIdentifier());

    List<Task> tasks = taskRepository.findTasksByAction_IdentifierAndLocation_Identifier(
        taskRequest.getActionIdentifier(), taskRequest.getLocationIdentifier());

    if (!tasks.isEmpty()) {
      throw new DuplicateTaskCreationException(
          "Task for action id ".concat(taskRequest.getActionIdentifier().toString())
              .concat(" and ").concat(taskRequest.getLocationIdentifier().toString())
              .concat(" already exists"));
    }

    Task task = TaskEntityFactory.entityFromRequestObj(taskRequest, action, lookupTaskStatus);

    if (taskRequest.getLocationIdentifier() != null) {
      Location location = locationService.findByIdentifier(taskRequest.getLocationIdentifier());
      task.setLocation(location);
    }

    task.setEntityStatus(EntityStatus.ACTIVE);
    return taskRepository.save(task);
  }

  public Task getTaskByIdentifier(UUID identifier) {
    return taskRepository.findByIdentifier(identifier)
        .orElseThrow(() -> new NotFoundException(Pair.of(
            Fields.identifier, identifier), Task.class));
  }

  public Task updateTask(UUID identifier, TaskUpdateRequest taskUpdateRequest) {

    Task taskToUpdate = getTaskByIdentifier(identifier);

    LookupTaskStatus lookupTaskStatus = lookupTaskStatusRepository.getById(
        taskUpdateRequest.getLookupTaskStatus());

    taskToUpdate.setLookupTaskStatus(lookupTaskStatus);
    taskToUpdate.setLastModified(LocalDateTime.now());
    taskToUpdate.setDescription(taskUpdateRequest.getDescription());
    taskToUpdate.setExecutionPeriodStart(taskUpdateRequest.getExecutionPeriodStart());
    taskToUpdate.setExecutionPeriodEnd(
        taskUpdateRequest.getExecutionPeriodEnd());
    taskToUpdate.setPriority(taskUpdateRequest.getPriority());

    return taskRepository.save(taskToUpdate);
  }

  public List<LookupTaskStatus> getAllTaskStatus() {
    return lookupTaskStatusRepository.findAll();
  }

  public void generateTasksByPlanId(UUID planIdentifier) throws QueryGenerationException {

    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    if (plan.getStatus().equals(PlanStatusEnum.ACTIVE)) {

      plan.getGoals().stream().forEach(goal ->
          goal.getActions().forEach(action ->
              action.getConditions()
                  .forEach(
                      condition -> {
                        try {
                          generateTasksByActionConditionQuery(action, condition,
                              planIdentifier);
                        } catch (QueryGenerationException e) {
                          log.error("Cannot generate tasks for condition: {}, action: {}, plan: {}",condition,action,plan);
                        }
                      }))
      );

    } else {
      log.info("Cannot generate tasks for plan with identifier: {} as it not yet been activated",
          planIdentifier);
    }
  }

  public void generateTasksByActionConditionQuery(Action action,
      com.revealprecision.revealserver.persistence.domain.Condition condition,
      UUID planIdentifier) throws QueryGenerationException {
    Query query = ConditionQueryUtil.getQueryObject(condition.getQuery(),
        action.getLookupEntityType().getCode());

    List<UUID> uuids = entityFilterService.filterEntities(query, planIdentifier);
    List<Task> tasks = new ArrayList<>();
    for (UUID entityUUID : uuids) {

      if (action.getLookupEntityType().getCode().equals(LOCATION)) {
        if (taskRepository.findTasksByAction_IdentifierAndLocation_Identifier(
            action.getIdentifier(), entityUUID).size() > 0) {
          log.info("task for location: {} already exists", entityUUID);
          continue;
        }
      }

      if (action.getLookupEntityType().getCode().equals(PERSON)) {
        if (taskRepository.findTasksByAction_IdentifierAndPerson_Identifier(
            action.getIdentifier(),
            entityUUID).size() > 0) {
          log.info("task for person: {} already exists", entityUUID);
          continue;
        }
      }

      Task task = Task.builder()
          .lookupTaskStatus(lookupTaskStatusRepository.findByCode("READY").orElseThrow(
              () -> new NotFoundException(Pair.of(LookupTaskStatus.Fields.code, "READY"),
                  LookupTaskStatus.class)))
          .priority(TaskPriorityEnum.ROUTINE)
          .description(action.getDescription())
          .lastModified(LocalDateTime.now())
          .authoredOn(LocalDateTime.now())
          .action(action)
          .executionPeriodStart(
              action.getTimingPeriodStart())
          .executionPeriodEnd(action.getTimingPeriodEnd())
          .build();
      task.setEntityStatus(EntityStatus.ACTIVE);

      if (action.getLookupEntityType().getCode().equals(LOCATION)) {
        Location location = locationService.findByIdentifier(entityUUID);
        task.setLocation(location);
      }
      if (action.getLookupEntityType().getCode().equals(PERSON)) {
        Person person = personService.getPersonByIdentifier(entityUUID);
        task.setPerson(person);
      }

      tasks.add(task);
    }
    log.info("no of tasks to be generate for action: {} and condition: {} is: {}",
        action.getTitle(), condition.getName(), tasks.size());
    taskRepository.saveAll(tasks);
  }
}