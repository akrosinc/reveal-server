package com.revealprecision.revealserver.service;

import static com.revealprecision.revealserver.constants.LocationConstants.STRUCTURE;

import com.revealprecision.revealserver.api.v1.dto.factory.TaskEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.TaskCreateRequest;
import com.revealprecision.revealserver.api.v1.dto.request.TaskUpdateRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.LookupEntityTypeCodeEnum;
import com.revealprecision.revealserver.enums.PlanStatusEnum;
import com.revealprecision.revealserver.enums.TaskPriorityEnum;
import com.revealprecision.revealserver.exceptions.DuplicateTaskCreationException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.QueryGenerationException;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Condition;
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
import com.revealprecision.revealserver.util.ActionUtils;
import com.revealprecision.revealserver.util.ConditionQueryUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  public TaskService(TaskRepository taskRepository, PlanService planService,
      ActionService actionService, LocationService locationService,
      LookupTaskStatusRepository lookupTaskStatusRepository, PersonService personService,
      EntityFilterService entityFilterService, GoalService goalService,
      ConditionService conditionService, PlanLocationsService planLocationsService,
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
          "Task for action id ".concat(taskRequest.getActionIdentifier().toString()).concat(" and ")
              .concat(taskRequest.getLocationIdentifier().toString()).concat(" already exists"));
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
    return taskRepository.findByIdentifier(identifier).orElseThrow(
        () -> new NotFoundException(Pair.of(Fields.identifier, identifier), Task.class));
  }

  public Task updateTask(UUID identifier, TaskUpdateRequest taskUpdateRequest) {

    Task taskToUpdate = getTaskByIdentifier(identifier);

    LookupTaskStatus lookupTaskStatus = lookupTaskStatusRepository.getById(
        taskUpdateRequest.getLookupTaskStatus());

    taskToUpdate.setLookupTaskStatus(lookupTaskStatus);
    taskToUpdate.setLastModified(LocalDateTime.now());
    taskToUpdate.setDescription(taskUpdateRequest.getDescription());
    taskToUpdate.setExecutionPeriodStart(taskUpdateRequest.getExecutionPeriodStart());
    taskToUpdate.setExecutionPeriodEnd(taskUpdateRequest.getExecutionPeriodEnd());
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

      List<Action> actions = goals.stream()
          .map(goal -> actionService.getActionsByGoalIdentifier(goal.getIdentifier()))
          .flatMap(Collection::stream).collect(Collectors.toList());

      Map<Action, List<Condition>> actionToConditions = new HashMap<>();
      actions.forEach(action -> {
        List<Condition> conditions = conditionService.getConditionsByActionIdentifier(
            action.getIdentifier());
        actionToConditions.put(action, conditions);
      });

      actionToConditions.forEach((action, conditions) -> {
        if (conditions.isEmpty()) {
          generateTasksUnconditionally(action, plan);
        } else {
          try {
            generateTasksByActionConditionQuery(action, conditions.get(0),
                plan); //Assume 1-1 action to condition,maybe change entity to reflect such
          } catch (QueryGenerationException e) {
            log.error("Cannot generate tasks for condition: {}, action: {}, plan: {}",
                conditions.get(0), action, plan);
            e.printStackTrace();
          }
        }

      });
    }
  }

  public void generateTasksByActionConditionQuery(Action action,
      com.revealprecision.revealserver.persistence.domain.Condition condition, Plan plan)
      throws QueryGenerationException {
    Query query = ConditionQueryUtil.getQueryObject(condition.getQuery(),
        action.getLookupEntityType().getCode());

    List<UUID> uuids = entityFilterService.filterEntities(query, plan.getIdentifier(),
        plan.getLocationHierarchy().getIdentifier());
    List<Task> tasks = new ArrayList<>();
    for (UUID entityUUID : uuids) {

      Task task = createTaskObjectFromActionAndEntityId(action, entityUUID, plan);
      if (task == null) {
        continue;
      }

      tasks.add(task);
    }
    log.info("no of tasks to be generate for action: {} and condition: {} is: {}",
        action.getTitle(), condition.getName(), tasks.size());
    taskRepository.saveAll(tasks);
  }

  public void generateTasksUnconditionally(Action action, Plan plan) {
    //TODO: update this once business rule for unconditional generation of tasks linked to person has been defined.
    Boolean isActionForLocation = ActionUtils.isActionForLocation(action);
    if (isActionForLocation) {
      Set<Location> taskLocations = plan.getPlanLocations().stream().map(PlanLocations::getLocation)
          .collect(Collectors.toSet());
      Set<Location> structureLocations = locationRelationshipService
          .getStructuresForPlanIfHierarchyHasStructure(
              plan.getLocationHierarchy(), taskLocations);
      taskLocations.addAll(structureLocations);
      generateTasksForLocationEntities(action, plan, taskLocations);
    }
  }

  private void generateTasksForLocationEntities(Action action, Plan plan, Set<Location> entities) {
    List<Task> tasks = new ArrayList<>();
    entities.forEach(entity -> {
      Task task = createTaskObjectFromActionAndEntityId(action, entity.getIdentifier(), plan);
      if (task != null) {
        tasks.add(task);
      }
    });
    if (!tasks.isEmpty()) {
      taskRepository.saveAll(tasks);
    }
  }

  private Task createTaskObjectFromActionAndEntityId(Action action, UUID entityUUID, Plan plan) {
    boolean isActionForLocation = ActionUtils.isActionForLocation(action);
    if (isActionForLocation) {
      if (taskRepository.findTasksByAction_IdentifierAndLocation_Identifier(action.getIdentifier(),
          entityUUID).size() > 0) {
        log.info("task for location: {} already exists", entityUUID);
        return null;
      }
    }

    boolean isActionForPerson = ActionUtils.isActionForPerson(action);
    if (isActionForPerson) {
      if (taskRepository.findTasksByAction_IdentifierAndPerson_Identifier(action.getIdentifier(),
          entityUUID).size() > 0) {
        log.info("task for person: {} already exists", entityUUID);
        return null;
      }
    }

    Task task = Task.builder().lookupTaskStatus(
        lookupTaskStatusRepository.findByCode(TASK_STATUS_READY).orElseThrow(
            () -> new NotFoundException(Pair.of(LookupTaskStatus.Fields.code, TASK_STATUS_READY),
                LookupTaskStatus.class))).priority(TaskPriorityEnum.ROUTINE)
        .description(action.getDescription()).lastModified(LocalDateTime.now())
        .authoredOn(LocalDateTime.now()).baseEntityIdentifier(entityUUID).action(action)
        .executionPeriodStart(action.getTimingPeriodStart())
        .executionPeriodEnd(action.getTimingPeriodEnd()).plan(plan).build();
    task.setEntityStatus(EntityStatus.ACTIVE);

    if (isActionForLocation) {
      Location location = locationService.findByIdentifier(entityUUID);
      task.setLocation(location);
    }
    if (isActionForPerson) {
      Person person = personService.getPersonByIdentifier(entityUUID);
      task.setPerson(person);
    }
    task.setServerVersion(0L);
    return task;
  }

  public void updateOrganizationsAndLocationsForTasksByPlanIdentifier(UUID planIdentifier) {

    List<Task> tasksByPlan = taskRepository.findTasksByPlan_Identifier(planIdentifier);
//TODO - resolve the dependency injection issue so that @Lazy is not used in this class
    Plan plan = planService.getPlanByIdentifier(planIdentifier);

    LookupTaskStatus lookupTaskStatus = lookupTaskStatusRepository.findByCode(TASK_STATUS_CANCELLED)
        .orElseThrow(() -> new NotFoundException(
            Pair.of(LookupTaskStatus.Fields.code, TASK_STATUS_CANCELLED), LookupTaskStatus.class));

    for (Task task : tasksByPlan) {

      updateOrganisationsAndLocationsForTask(plan, lookupTaskStatus, task);
    }
  }

  public void updateOrganisationsAndLocationsForTask(Plan plan,
      LookupTaskStatus lookupTaskStatus, Task task) {
    Action action = task.getAction();
    List<PlanLocations> planLocationsForLocation = new ArrayList<>();
    boolean isActionForLocation = ActionUtils.isActionForLocation(action);
    if (isActionForLocation) {
      if (task.getLocation() != null) {
        if (task.getLocation().getGeographicLevel().getName().equals(STRUCTURE)) {
          Location parentLocation = locationService
              .getLocationParent(task.getLocation(), plan.getLocationHierarchy());
          planLocationsForLocation = planLocationsService.getPlanLocationsByLocationIdentifier(
              parentLocation.getIdentifier());
        } else {
          planLocationsForLocation = planLocationsService.getPlanLocationsByLocationIdentifier(
              task.getLocation().getIdentifier());
        }
      }
    }

    List<PlanLocations> planLocationsForPerson = new ArrayList<>();
    boolean isActionForPerson = ActionUtils.isActionForPerson(action);
    if (isActionForPerson && task.getPerson() != null && task.getPerson().getLocations() != null) {

      if (task.getLocation().getGeographicLevel().getName().equals(STRUCTURE)) {
        Location parentLocation = locationService
            .getLocationParent(task.getLocation(), plan.getLocationHierarchy());
        planLocationsForPerson = planLocationsService.getPlanLocationsByLocationIdentifier(
            parentLocation.getIdentifier());
      } else {
        planLocationsForPerson = planLocationsService.getPlanLocationsByLocationIdentifierList(
            task.getPerson().getLocations().stream().map(Location::getIdentifier)
                .collect(Collectors.toList()));
      }
    }

    if (planLocationsForLocation.isEmpty() && planLocationsForPerson.isEmpty()) {
      task.setLookupTaskStatus(lookupTaskStatus);
    } else {
      List<Organization> organizations = new ArrayList<>();
      if (isActionForLocation) {
        if (planLocationsForLocation.size() > 0) {
          organizations = getOrganizationsFromAssignedLocations(plan.getIdentifier(),
              planLocationsForLocation);
        }
      }
      if (isActionForPerson) {
        if (planLocationsForPerson.size() > 0) {
          organizations = getOrganizationsFromAssignedLocations(plan.getIdentifier(),
              planLocationsForPerson);
        }
      }
      task.setOrganizations(organizations);
    }
    taskRepository.save(task);
  }

  private List<Organization> getOrganizationsFromAssignedLocations(UUID planIdentifier,
      List<PlanLocations> planLocationsForPerson) {
    return planLocationsForPerson.stream()
        .filter(planLocations1 -> planLocations1.getPlan().getIdentifier().equals(planIdentifier))
        .flatMap(planLocation -> planLocation.getPlanAssignments().stream())
        .map(PlanAssignment::getOrganization).collect(Collectors.toList());
  }

  public Map<UUID, List<Task>> getTasksPerJurisdictionIdentifier(UUID planIdentifier,
      List<UUID> jurisdictionIdentifiers) {
    Map<UUID, List<Task>> tasksToJurisdictions = new HashMap<>();
    try {
      Plan plan = planService.getPlanByIdentifier(planIdentifier);
      LocationHierarchy locationHierarchy = plan.getLocationHierarchy();

      jurisdictionIdentifiers.forEach(jurisdictionIdentifier -> {
        List<Location> childLocations = locationRelationshipService
            .getLocationChildrenByLocationParentIdentifierAndHierarchyIdentifier(
                List.of(jurisdictionIdentifier), locationHierarchy.getIdentifier());

        List<UUID> baseEntityIdentifiers = new ArrayList<>();

        //TODO: can this be done in a single query from below findByPlanAndBaseEntityIdentifiers method
        List<UUID> personIdentifiers = personService.getPeopleByLocations(childLocations)
            .stream()
            .map(Person::getIdentifier).collect(Collectors.toList());
        baseEntityIdentifiers.addAll(personIdentifiers);

        baseEntityIdentifiers.addAll(
            childLocations.stream().map(Location::getIdentifier).collect(Collectors.toList()));

        List<Task> tasks = taskRepository.findByPlanAndBaseEntityIdentifiers(plan
            , baseEntityIdentifiers);
        if (!tasks.isEmpty()) {
          tasksToJurisdictions.put(jurisdictionIdentifier, tasks);
        }

      });
    } catch (NotFoundException e) {
      e.printStackTrace();
    }
    return tasksToJurisdictions;
  }
}