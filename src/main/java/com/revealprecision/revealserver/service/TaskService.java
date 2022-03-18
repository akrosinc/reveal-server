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
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LookupTaskStatus;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.PlanAssignment;
import com.revealprecision.revealserver.persistence.domain.PlanLocations;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TaskService {

  private static final String ENTITY_LOCATION = "Location";
  private static final String ENTITY_PERSON = "Person";
  public static final String TASK_STATUS_READY = "READY";
  public static final String TASK_STATUS_CANCELLED = "CANCELLED";
  private final TaskRepository taskRepository;
  private final PlanLocationsService planLocationsService;

  private final PlanService planService;
  private final ActionService actionService;
  private final PersonService personService;
  private final GoalService goalService;
  private final ConditionService conditionService;

  private final LocationService locationService;
  private final LookupTaskStatusRepository lookupTaskStatusRepository;
  private final EntityFilterService entityFilterService;
  private final LocationRelationshipService locationRelationshipService;

  @Autowired
  @Lazy
  public TaskService(TaskRepository taskRepository,
      PlanService planService,
      ActionService actionService, LocationService locationService,
      LookupTaskStatusRepository lookupTaskStatusRepository, PersonService personService,
      EntityFilterService entityFilterService, GoalService goalService,
      ConditionService conditionService,
      PlanLocationsService planLocationsService,
      LocationRelationshipService locationRelationshipService) {
    this.taskRepository = taskRepository;
    this.planService = planService;
    this.actionService = actionService;
    this.locationService = locationService;
    this.lookupTaskStatusRepository = lookupTaskStatusRepository;
    this.personService = personService;
    this.entityFilterService = entityFilterService;
    this.goalService = goalService;
    this.conditionService = conditionService;
    this.planLocationsService = planLocationsService;
    this.locationRelationshipService = locationRelationshipService;

  }

  public Page<Task> searchTasks(TaskSearchCriteria taskSearchCriteria, Pageable pageable) {
    return taskRepository.findAll(TaskSpec.getTaskSpecification(taskSearchCriteria), pageable);
  }

  public Long countTasksBySearchCriteria(TaskSearchCriteria taskSearchCriteria) {
    return taskRepository.count(TaskSpec.getTaskSpecification(taskSearchCriteria));
  }

  public Task saveTask(Task task) {
    return taskRepository.save(task);
  }

  public List<Task> saveTasks(List<Task> tasks) {
    return taskRepository.saveAll(tasks);
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

  public void generateTasksByPlanId(UUID planIdentifier) {

    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    if (plan.getStatus().equals(PlanStatusEnum.ACTIVE)) {
      List<Goal> goals = goalService.getGoalsByPlanIdentifier(planIdentifier);

      goals.forEach(goal ->
          actionService.getActionsByGoalIdentifier(goal.getIdentifier()).forEach(action ->
              conditionService.getConditionsByActionIdentifier(action.getIdentifier())
                  .forEach(
                      condition -> {
                        try {
                          generateTasksByActionConditionQuery(action, condition,
                              plan);
                        } catch (QueryGenerationException e) {
                          log.error("Cannot generate tasks for condition: {}, action: {}, plan: {}",
                              condition, action, plan);
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
      Plan plan) throws QueryGenerationException {
    Query query = ConditionQueryUtil.getQueryObject(condition.getQuery(),
        action.getLookupEntityType().getCode());

    List<UUID> uuids = entityFilterService.filterEntities(query, plan.getIdentifier());
    List<Task> tasks = new ArrayList<>();
    for (UUID entityUUID : uuids) {

      Task task = createTaskObjectFromActionAndEntityId(action, entityUUID,
          plan);
      if (task == null) {
        continue;
      }

      tasks.add(task);
    }
    log.info("no of tasks to be generate for action: {} and condition: {} is: {}",
        action.getTitle(), condition.getName(), tasks.size());
    taskRepository.saveAll(tasks);
  }

  private Task createTaskObjectFromActionAndEntityId(Action action,
      UUID entityUUID, Plan plan) {
    boolean isLocationEntity = action.getLookupEntityType() != null && ENTITY_LOCATION
        .equals(action.getLookupEntityType().getCode());
    if (isLocationEntity) {
      if (taskRepository.findTasksByAction_IdentifierAndLocation_Identifier(
          action.getIdentifier(), entityUUID).size() > 0) {
        log.info("task for location: {} already exists", entityUUID);
        return null;
      }
    }

    boolean isPersonEntity = action.getLookupEntityType() != null && ENTITY_PERSON
        .equals(action.getLookupEntityType().getCode());
    if (isPersonEntity) {
      if (taskRepository.findTasksByAction_IdentifierAndPerson_Identifier(
          action.getIdentifier(),
          entityUUID).size() > 0) {
        log.info("task for person: {} already exists", entityUUID);
        return null;
      }
    }

    Task task = Task.builder()
        .lookupTaskStatus(lookupTaskStatusRepository.findByCode(TASK_STATUS_READY).orElseThrow(
            () -> new NotFoundException(Pair.of(LookupTaskStatus.Fields.code, TASK_STATUS_READY),
                LookupTaskStatus.class)))
        .priority(TaskPriorityEnum.ROUTINE)
        .description(action.getDescription())
        .lastModified(LocalDateTime.now())
        .authoredOn(LocalDateTime.now())
        .baseEntityIdentifier(entityUUID)
        .action(action)
        .executionPeriodStart(
            action.getTimingPeriodStart())
        .executionPeriodEnd(action.getTimingPeriodEnd())
        .plan(plan)
        .build();
    task.setEntityStatus(EntityStatus.ACTIVE);

    if (isLocationEntity) {
      Location location = locationService.findByIdentifier(entityUUID);
      task.setLocation(location);
    }
    if (isPersonEntity) {
      Person person = personService.getPersonByIdentifier(entityUUID);
      task.setPerson(person);
    }
    return task;
  }

  public void updateOrganizationsAndLocationsForTasksByPlanIdentifier(UUID planIdentifier) {

    List<Task> tasksByPlan = taskRepository.findTasksByPlan_Identifier(planIdentifier);

    LookupTaskStatus lookupTaskStatus = lookupTaskStatusRepository.findByCode(TASK_STATUS_CANCELLED)
        .orElseThrow(() -> new NotFoundException(
            Pair.of(LookupTaskStatus.Fields.code, TASK_STATUS_CANCELLED), LookupTaskStatus.class));

    for (Task task : tasksByPlan) {

      updateOrganisationsAndLocationsForTask(planIdentifier, lookupTaskStatus, task);
    }
  }

  public void updateOrganisationsAndLocationsForTask(UUID planIdentifier,
      LookupTaskStatus lookupTaskStatus, Task task) {
    Action action = task.getAction();
    List<PlanLocations> planLocationsForLocation = new ArrayList<>();
    boolean isLocationEntity = action.getLookupEntityType().getCode().equals(ENTITY_LOCATION);
    if (isLocationEntity) {
      if (task.getLocation() != null) {
        planLocationsForLocation = planLocationsService.getPlanLocationsByLocationIdentifier(
            task.getLocation().getIdentifier());
      }
    }

    List<PlanLocations> planLocationsForPerson = new ArrayList<>();
    boolean isPersonEntity = action.getLookupEntityType().getCode().equals(ENTITY_PERSON);
    if (isPersonEntity) {
      if (task.getPerson() != null) {
        planLocationsForPerson = planLocationsService.getPlanLocationsByLocationIdentifierList(
            task.getPerson().getLocations().stream()
                .map(Location::getIdentifier)
                .collect(Collectors.toList()));
      }
    }

    if (planLocationsForLocation.isEmpty() && planLocationsForPerson.isEmpty()) {
      task.setLookupTaskStatus(lookupTaskStatus);
    } else {
      List<Organization> organizations = new ArrayList<>();
      if (isLocationEntity) {
        if (planLocationsForLocation.size() > 0) {
           organizations = getOrganizationsFromAssignedLocations(planIdentifier,
               planLocationsForLocation);
        }
      }
      if (isPersonEntity) {
        if (planLocationsForPerson.size() > 0) {
           organizations = getOrganizationsFromAssignedLocations(planIdentifier,
               planLocationsForPerson);
        }
      }
      task.setOrganizations(organizations);
    }
    taskRepository.save(task);
  }

  private List<Organization> getOrganizationsFromAssignedLocations(UUID planIdentifier,
      List<PlanLocations> planLocationsForPerson) {
    return planLocationsForPerson.stream().filter(
        planLocations1 -> planLocations1.getPlan().getIdentifier().equals(planIdentifier))
        .flatMap(planLocation -> planLocation.getPlanAssignments().stream())
        .map(PlanAssignment::getOrganization)
        .collect(Collectors.toList());
  }

  public Map<UUID, List<Task>> getTasksPerJurisdictionIdentifier(UUID planIdentifier,
      List<UUID> jurisdictionIdentifiers) {
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    LocationHierarchy locationHierarchy = plan.getLocationHierarchy();
    Map<UUID, List<Task>> tasksToJurisdictions = new HashMap<>();

    jurisdictionIdentifiers.stream().forEach(jurisdictionIdentifier -> {
      List<Location> childLocations = locationRelationshipService
          .getLocationChildrenByLocationParentIdentifierAndHierarchyIdentifier(
              List.of(jurisdictionIdentifier), locationHierarchy.getIdentifier());

      List<Task> tasks = taskRepository.findByPlanAndBaseEntityIdentifiers(plan,
          childLocations.stream().map(Location::getIdentifier).collect(
              Collectors.toList()));
      if (!tasks.isEmpty()) {
        tasksToJurisdictions.put(jurisdictionIdentifier, tasks);
      }
    });
    return tasksToJurisdictions;
  }
}