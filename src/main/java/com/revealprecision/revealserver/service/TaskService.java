package com.revealprecision.revealserver.service;

import static com.revealprecision.revealserver.constants.LocationConstants.STRUCTURE;

import com.revealprecision.revealserver.api.v1.dto.factory.TaskEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.TaskCreateRequest;
import com.revealprecision.revealserver.api.v1.dto.request.TaskUpdateRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.PlanStatusEnum;
import com.revealprecision.revealserver.enums.TaskPriorityEnum;
import com.revealprecision.revealserver.exceptions.DuplicateTaskCreationException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.QueryGenerationException;
import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.TaskEventFactory;
import com.revealprecision.revealserver.messaging.message.Message;
import com.revealprecision.revealserver.messaging.message.TaskEvent;
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
import com.revealprecision.revealserver.props.BusinessStatusProperties;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.models.TaskSearchCriteria;
import com.revealprecision.revealserver.util.ActionUtils;
import com.revealprecision.revealserver.util.ConditionQueryUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.kafka.core.KafkaTemplate;
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
  private final BusinessStatusProperties businessStatusProperties;
  private final BusinessStatusService businessStatusService;
  private final KafkaTemplate<String, Message> kafkaTemplate;
  private final KafkaProperties kafkaProperties;

  @Autowired
  @Lazy
  public TaskService(TaskRepository taskRepository, PlanService planService,
      ActionService actionService, LocationService locationService,
      LookupTaskStatusRepository lookupTaskStatusRepository, PersonService personService,
      EntityFilterService entityFilterService, GoalService goalService,
      ConditionService conditionService, PlanLocationsService planLocationsService,
      LocationRelationshipService locationRelationshipService,
      BusinessStatusProperties businessStatusProperties,
      BusinessStatusService businessStatusService,
      KafkaTemplate<String, Message> kafkaTemplate,
      KafkaProperties kafkaProperties) {
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
    this.businessStatusProperties = businessStatusProperties;
    this.businessStatusService = businessStatusService;
    this.kafkaTemplate = kafkaTemplate;
    this.kafkaProperties = kafkaProperties;
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

  public void generateTasksByPlanId(UUID planIdentifier, String ownerId) {

    log.info("TASK_GENERATION Start generate tasks for Plan Id: {}", planIdentifier);
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    if (plan.getStatus().equals(PlanStatusEnum.ACTIVE)) {
      List<Goal> goals = goalService.getGoalsByPlanIdentifier(planIdentifier);

      goals.stream().map(goal -> actionService.getActionsByGoalIdentifier(goal.getIdentifier()))
          .flatMap(Collection::stream)
          .forEach((action) -> generateTasksByAction(action, plan, ownerId));
    }
    log.info("TASK_GENERATION Completed generating tasks for Plan Id: {}", planIdentifier);
  }

  public void cancelApplicableTasksByPlanId(UUID planIdentifier, String ownerId) {

    log.info("TASK_CANCELLATION Start cancellation for tasks for Plan Id: {}", planIdentifier);
    LookupTaskStatus cancelledLookupTaskStatus = lookupTaskStatusRepository.findByCode(
            TASK_STATUS_CANCELLED)
        .orElseThrow(() -> new NotFoundException(
            Pair.of(LookupTaskStatus.Fields.code, TASK_STATUS_CANCELLED), LookupTaskStatus.class));
    List<Task> tasksByPlan = taskRepository.findTasksByPlan_Identifier(planIdentifier);
    Plan plan = planService.getPlanByIdentifier(planIdentifier);
    if (plan.getStatus().equals(PlanStatusEnum.ACTIVE)) {
      List<Task> tasksToCancel = tasksByPlan.stream()
          .map((task) -> markUnassignedTaskAsCancelled(plan, cancelledLookupTaskStatus, task))
          .filter(Objects::nonNull)
          .filter(task -> task.getLookupTaskStatus().getCode()
              .equals(cancelledLookupTaskStatus.getCode()))
          .collect(Collectors.toList());
      List<Task> savedCancelledTask = taskRepository.saveAll(tasksToCancel);

      for (Task task : savedCancelledTask) {
        TaskEvent taskEvent = TaskEventFactory.getTaskEventFromTask(task);
        //TODO: we need to save the owner in the database, just so we can retrieve it here
        taskEvent.setOwnerId(ownerId);
        kafkaTemplate.send(kafkaProperties.getTopicMap().get(KafkaConstants.TASK), taskEvent);
      }
    } else {
      log.info("Cannot cancel tasks as plan is not active");
    }
    log.info("TASK_CANCELLATION Completed cancellation tasks for Plan Id: {}", planIdentifier);
  }


  public void generateTasksByAction(Action action, Plan plan, String ownerId) {

    List<Condition> conditions = conditionService.getConditionsByActionIdentifier(
        action.getIdentifier());

    List<UUID> uuids = new ArrayList<>();

    if (conditions == null || conditions.isEmpty()) {
      try {
        uuids = entityFilterService.filterEntities(null, plan.getIdentifier(),
            plan.getLocationHierarchy().getIdentifier(), action);


      } catch (QueryGenerationException e) {
        log.error("unable to get tasks unconditionally for action: {}", action.getIdentifier());
        e.printStackTrace();
      }
    } else {
      for (Condition condition : conditions) {
        Query query = ConditionQueryUtil.getQueryObject(condition.getQuery(),
            action.getLookupEntityType().getCode());

        try {
          List<UUID> filteredUUIDs = entityFilterService.filterEntities(query, plan.getIdentifier(),
              plan.getLocationHierarchy().getIdentifier(), action);

          uuids.addAll(filteredUUIDs);

        } catch (QueryGenerationException e) {
          log.error("unable to get tasks for action: {} condition: {}", action.getIdentifier(),
              condition.getIdentifier());
          e.printStackTrace();
        }
      }
    }

    List<Task> tasks = new ArrayList<>();
    LookupTaskStatus cancelledLookupTaskStatus = lookupTaskStatusRepository.findByCode(
        TASK_STATUS_CANCELLED).orElseThrow(
        () -> new NotFoundException(Pair.of(LookupTaskStatus.Fields.code, TASK_STATUS_CANCELLED),
            LookupTaskStatus.class));

    LookupTaskStatus readyLookupTaskStatus = lookupTaskStatusRepository.findByCode(
        TASK_STATUS_READY).orElseThrow(
        () -> new NotFoundException(Pair.of(LookupTaskStatus.Fields.code, TASK_STATUS_READY),
            LookupTaskStatus.class));

    log.debug("entityUUID list: {}", uuids);
    for (UUID entityUUID : uuids) {

      List<Task> taskObjs = createOrUpdateTaskObjectFromActionAndEntityId(action,
          cancelledLookupTaskStatus, readyLookupTaskStatus, entityUUID, plan);
      if (taskObjs.isEmpty()) {
        continue;
      }
      tasks.addAll(taskObjs);
    }

    log.debug("no of tasks to be generate for action: {} is: {}", action.getTitle(), tasks.size());
    List<Task> savedTasks = taskRepository.saveAll(tasks);

    for (Task task : savedTasks) {
      log.debug("task: {} entity: {}", task.getIdentifier(), task.getBaseEntityIdentifier());
      TaskEvent taskEvent = TaskEventFactory.getTaskEventFromTask(task);
      taskEvent.setOwnerId(ownerId);
      kafkaTemplate.send(kafkaProperties.getTopicMap().get(KafkaConstants.TASK), taskEvent);
    }
  }


  private List<Task> createOrUpdateTaskObjectFromActionAndEntityId(Action action,
      LookupTaskStatus cancelledLookupTaskStatus, LookupTaskStatus readyLookupTaskStatus,
      UUID entityUUID, Plan plan) {
    log.debug("TASK_GENERATION  create individual task for plan: {} and action: {}",
        plan.getIdentifier(), action.getIdentifier());

    boolean isActionForLocation = ActionUtils.isActionForLocation(action);

    List<Task> existingLocationTasks = taskRepository.findTasksByAction_IdentifierAndLocation_Identifier(
        action.getIdentifier(), entityUUID);

    if (existingLocationTasks.size() > 0) {
      if (isActionForLocation) {
        for (Task existingLocationTask : existingLocationTasks) {
          if (existingLocationTask.getLookupTaskStatus().getCode()
              .equals(cancelledLookupTaskStatus.getCode())) {
            log.info("task for location: {} already exists", entityUUID);
            existingLocationTask.setLookupTaskStatus(readyLookupTaskStatus);
          }
        }
        return existingLocationTasks;
      }
    }

    List<Task> existingPersonTasks = taskRepository.findTasksByAction_IdentifierAndPerson_Identifier(
        action.getIdentifier(), entityUUID);
    boolean isActionForPerson = ActionUtils.isActionForPerson(action);

    if (existingPersonTasks.size() > 0) {

      if (isActionForPerson) {
        for (Task personTask : existingPersonTasks) {
          if (personTask.getLookupTaskStatus().getCode()
              .equals(cancelledLookupTaskStatus.getCode())) {
            log.info("task for person: {} already exists", entityUUID);
            personTask.setLookupTaskStatus(readyLookupTaskStatus);
          }
        }
        return existingPersonTasks;
      }
    }

    Task task = Task.builder().lookupTaskStatus(
            lookupTaskStatusRepository.findByCode(TASK_STATUS_READY).orElseThrow(
                () -> new NotFoundException(Pair.of(LookupTaskStatus.Fields.code, TASK_STATUS_READY),
                    LookupTaskStatus.class))).priority(TaskPriorityEnum.ROUTINE)
        .description(action.getDescription()).lastModified(LocalDateTime.now())
        .authoredOn(LocalDateTime.now()).baseEntityIdentifier(entityUUID).action(action)
        .executionPeriodStart(action.getTimingPeriodStart())
        //TODO how to get this before save unless we do it on save of the task with kafka
        .identifier(UUID.randomUUID()).executionPeriodEnd(action.getTimingPeriodEnd()).plan(plan)
        .build();
    task.setBusinessStatus(businessStatusProperties.getDefaultLocationBusinessStatus());

    task.setEntityStatus(EntityStatus.ACTIVE);

    if (isActionForLocation) {
      Location location = locationService.findByIdentifier(entityUUID);
      task.setLocation(location);
    }
    if (isActionForPerson) {
      Person person = personService.getPersonByIdentifier(entityUUID);
      task.setPerson(person);
    }

    businessStatusService.setBusinessStatus(task,
        businessStatusProperties.getDefaultLocationBusinessStatus());
    log.debug("TASK_GENERATION completed creating individual task for plan: {} and action: {}",
        plan.getIdentifier(), action.getIdentifier());

    return List.of(task);
  }


  public void updateOrganizationsAndLocationsForTasksByPlanIdentifier(UUID planIdentifier) {
    log.info("TASK_ASSIGN Start update tasks for Plan Id: {}", planIdentifier);

    List<Task> tasksByPlan = taskRepository.findTasksByPlan_Identifier(planIdentifier);
//TODO - resolve the dependency injection issue so that @Lazy is not used in this class
    Plan plan = planService.getPlanByIdentifier(planIdentifier);

    LookupTaskStatus lookupTaskStatus = lookupTaskStatusRepository.findByCode(TASK_STATUS_CANCELLED)
        .orElseThrow(() -> new NotFoundException(
            Pair.of(LookupTaskStatus.Fields.code, TASK_STATUS_CANCELLED), LookupTaskStatus.class));

    int taskListSize = tasksByPlan.size();
    int taskCount = 1;

    for (Task task : tasksByPlan) {
      log.debug("processing task {} - id: {} of total {}", taskCount, task.getIdentifier(),
          taskListSize);
      updateOrganisationsAndLocationsForTask(plan, lookupTaskStatus, task);
      taskCount++;
    }
    log.info("TASK_ASSIGN Completed updating tasks for Plan Id: {}", planIdentifier);

  }


  private Task markUnassignedTaskAsCancelled(Plan plan, LookupTaskStatus lookupTaskStatus,
      Task task) {
    log.debug("TASK_CANCEL Start updating location and organization assignments for task: {}",
        task.getIdentifier());
    Action action = task.getAction();

    log.debug("TASK_CANCEL getting plan locations: {}", task.getIdentifier());

    List<PlanLocations> planLocationsForLocation = getPlanLocationsForLocation(plan, task, action);

    List<PlanLocations> planLocationsForPerson = getPlanLocationsForPerson(plan, task, action);
    log.debug("TASK_CANCEL got plan locations: {}", task.getIdentifier());

    if (!task.getLookupTaskStatus().getCode().equals(lookupTaskStatus.getCode())) {
      if (planLocationsForLocation.isEmpty() && planLocationsForPerson.isEmpty()) {
        task.setLookupTaskStatus(lookupTaskStatus);
      }
    } else {
      return null;
    }
    return task;
  }

  public void updateOrganisationsAndLocationsForTask(Plan plan, LookupTaskStatus lookupTaskStatus,
      Task task) {
    taskRepository.save(
        getUpdatedTaskAfterLocationAndOrganizationAssignment(plan, lookupTaskStatus, task));
  }

  private Task getUpdatedTaskAfterLocationAndOrganizationAssignment(Plan plan,
      LookupTaskStatus lookupTaskStatus, Task task) {
    log.debug("TASK_ASSIGN Start updating location and organization assignments for task: {}",
        task.getIdentifier());
    Action action = task.getAction();

    log.debug("TASK_ASSIGN getting plan locations: {}", task.getIdentifier());

    List<PlanLocations> planLocationsForLocation = getPlanLocationsForLocation(plan, task, action);

    List<PlanLocations> planLocationsForPerson = getPlanLocationsForPerson(plan, task, action);
    log.debug("TASK_ASSIGN got plan locations: {}", task.getIdentifier());

    return updateTaskWithOrganizations(plan, lookupTaskStatus, task, planLocationsForLocation,
        planLocationsForPerson, action);
  }

  private Task updateTaskWithOrganizations(Plan plan, LookupTaskStatus lookupTaskStatus, Task task,
      List<PlanLocations> planLocationsForLocation, List<PlanLocations> planLocationsForPerson,
      Action action) {
    log.debug("TASK_ASSIGN getting organizations locations: {}", task.getIdentifier());

    boolean isActionForPerson = ActionUtils.isActionForPerson(action);

    boolean isActionForLocation = ActionUtils.isActionForLocation(action);

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
      log.debug("TASK_ASSIGN got organizations locations: {}", task.getIdentifier());
      task.setOrganizations(organizations);
    }
    log.debug("TASK_ASSIGN Completed updating location and organization assignments for task: {}",
        task.getIdentifier());
    return task;
  }

  private List<PlanLocations> getPlanLocationsForPerson(Plan plan, Task task, Action action) {

    List<PlanLocations> planLocationsForPerson = new ArrayList<>();

    boolean isActionForPerson = ActionUtils.isActionForPerson(action);

    if (isActionForPerson && task.getPerson() != null && task.getPerson().getLocations() != null) {

      if (task.getLocation().getGeographicLevel().getName().equals(STRUCTURE)) {
        Location parentLocation = locationService.getLocationParent(task.getLocation(),
            plan.getLocationHierarchy());
        planLocationsForPerson = planLocationsService.getPlanLocationsByPlanAndLocationIdentifier(
            parentLocation.getIdentifier(), plan.getIdentifier());
      } else {
        planLocationsForPerson = planLocationsService.getPlanLocationsByLocationIdentifierList(
            task.getPerson().getLocations().stream().map(Location::getIdentifier)
                .collect(Collectors.toList()));
      }
    }
    return planLocationsForPerson;
  }


  private List<PlanLocations> getPlanLocationsForLocation(Plan plan, Task task, Action action) {

    List<PlanLocations> planLocationsForLocation = new ArrayList<>();

    boolean isActionForLocation = ActionUtils.isActionForLocation(action);

    if (isActionForLocation) {
      if (task.getLocation() != null) {
        if (task.getLocation().getGeographicLevel().getName().equals(STRUCTURE)) {

          Location parentLocation = locationService.getLocationParent(task.getLocation(),
              plan.getLocationHierarchy());
          planLocationsForLocation = planLocationsService.getPlanLocationsByPlanAndLocationIdentifier(
              parentLocation.getIdentifier(), plan.getIdentifier());

        } else {
          planLocationsForLocation = planLocationsService.getPlanLocationsByPlanAndLocationIdentifier(
              task.getLocation().getIdentifier(), plan.getIdentifier());
        }
      }
    }
    return planLocationsForLocation;
  }


  private List<Organization> getOrganizationsFromAssignedLocations(UUID planIdentifier,
      List<PlanLocations> planLocationsForPerson) {
    return planLocationsForPerson.stream()
        .filter(planLocations1 -> planLocations1.getPlan().getIdentifier().equals(planIdentifier))
        .flatMap(planLocation -> planLocation.getPlanAssignments().stream())
        .map(PlanAssignment::getOrganization).collect(Collectors.toList());
  }

  public Map<UUID, List<Task>> getTasksPerJurisdictionIdentifier(UUID planIdentifier,
      List<UUID> jurisdictionIdentifiers, Long serverVersion) {
    Map<UUID, List<Task>> tasksToJurisdictions = new HashMap<>();
    try {
      Plan plan = planService.getPlanByIdentifier(planIdentifier);
      LocationHierarchy locationHierarchy = plan.getLocationHierarchy();

      jurisdictionIdentifiers.forEach(jurisdictionIdentifier -> {
        List<Location> childLocations = locationRelationshipService.getLocationChildrenByLocationParentIdentifierAndHierarchyIdentifier(
            List.of(jurisdictionIdentifier), locationHierarchy.getIdentifier());

        List<UUID> baseEntityIdentifiers = new ArrayList<>();

        //TODO: can this be done in a single query from below findByPlanAndBaseEntityIdentifiers method
        List<UUID> personIdentifiers = personService.getPeopleByLocations(childLocations).stream()
            .map(Person::getIdentifier).collect(Collectors.toList());
        baseEntityIdentifiers.addAll(personIdentifiers);

        baseEntityIdentifiers.addAll(
            childLocations.stream().map(Location::getIdentifier).collect(Collectors.toList()));

        List<Task> tasks = taskRepository.findByPlanAndBaseEntityIdentifiersAndMinimumServerVersion(
            plan, baseEntityIdentifiers, serverVersion);
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