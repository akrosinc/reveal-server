package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.controller.querying.KafkaGenerateIndividualTasksController.ListObj;
import com.revealprecision.revealserver.api.v1.dto.factory.TaskEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.TaskCreateRequest;
import com.revealprecision.revealserver.api.v1.dto.request.TaskUpdateRequest;
import com.revealprecision.revealserver.constants.FormConstants;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.enums.ActionTitleEnum;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.PlanInterventionTypeEnum;
import com.revealprecision.revealserver.enums.PlanStatusEnum;
import com.revealprecision.revealserver.enums.ProcessTrackerEnum;
import com.revealprecision.revealserver.enums.ProcessType;
import com.revealprecision.revealserver.enums.TaskGenerateRequestValidationStateEnum;
import com.revealprecision.revealserver.enums.TaskPriorityEnum;
import com.revealprecision.revealserver.enums.TaskProcessEnum;
import com.revealprecision.revealserver.exceptions.DuplicateTaskCreationException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.QueryGenerationException;
import com.revealprecision.revealserver.messaging.TaskEventFactory;
import com.revealprecision.revealserver.messaging.dto.TaskGen;
import com.revealprecision.revealserver.messaging.message.ActionEvent;
import com.revealprecision.revealserver.messaging.message.LookupEntityTypeEvent;
import com.revealprecision.revealserver.messaging.message.PlanEvent;
import com.revealprecision.revealserver.messaging.message.ProcessTrackerEvent;
import com.revealprecision.revealserver.messaging.message.TaskEvent;
import com.revealprecision.revealserver.messaging.message.TaskProcessEvent;
import com.revealprecision.revealserver.messaging.message.TaskProjectionObj;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Condition;
import com.revealprecision.revealserver.persistence.domain.EntityData;
import com.revealprecision.revealserver.persistence.domain.Goal;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LookupTaskStatus;
import com.revealprecision.revealserver.persistence.domain.Person;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.ProcessTracker;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.domain.Task.Fields;
import com.revealprecision.revealserver.persistence.domain.TaskProcessStage;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.domain.actioncondition.Query;
import com.revealprecision.revealserver.persistence.projection.TaskProjection;
import com.revealprecision.revealserver.persistence.repository.EntityDataRepository;
import com.revealprecision.revealserver.persistence.repository.LookupTaskStatusRepository;
import com.revealprecision.revealserver.persistence.repository.TaskProcessStageRepository;
import com.revealprecision.revealserver.persistence.repository.TaskRepository;
import com.revealprecision.revealserver.persistence.specification.TaskSpec;
import com.revealprecision.revealserver.props.BusinessStatusProperties;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.props.NoTaskActionProperties;
import com.revealprecision.revealserver.props.TaskGenerationProperties;
import com.revealprecision.revealserver.service.models.TaskSearchCriteria;
import com.revealprecision.revealserver.util.ActionUtils;
import com.revealprecision.revealserver.util.ConditionQueryUtil;
import com.revealprecision.revealserver.util.UserUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


@Service
@Slf4j
@RequiredArgsConstructor
public class TaskService {

  public static final String TASK_STATUS_READY = "READY";
  public static final String TASK_STATUS_CANCELLED = "CANCELLED";
  public static final String TASK_STATUS_COMPLETED = "COMPLETED";
  private final TaskRepository taskRepository;

  private final PlanService planService;
  private final ActionService actionService;
  private final PersonService personService;
  private final GoalService goalService;
  private final ConditionService conditionService;
  private final UserService userService;

  private final LocationService locationService;
  private final LookupTaskStatusRepository lookupTaskStatusRepository;
  private final EntityFilterService entityFilterService;
  private final BusinessStatusProperties businessStatusProperties;
  private final BusinessStatusService businessStatusService;
  private final PublisherService publisherService;
  private final KafkaProperties kafkaProperties;
  private final TaskProcessStageRepository taskProcessStageRepository;
  private final ProcessTrackerService processTrackerService;
  private final TaskGenerationProperties taskGenerationProperties;
  private final PlanLocationsService planLocationsService;

  private final NoTaskActionProperties noTaskActionProperties;

  private final EntityDataRepository entityDataRepository;

  @Getter
  private LookupTaskStatus cancelledLookupTaskStatus;
  @Getter
  private LookupTaskStatus readyLookupTaskStatus;
  @Getter
  private LookupTaskStatus completedLookupTaskStatus;

  public Page<Task> getAllTasksByPlan(UUID planIdentifier, Pageable pageable) {
    return taskRepository.findTasksByPlan_Identifier(planIdentifier, pageable);
  }


  public Page<Task> searchTasks(TaskSearchCriteria taskSearchCriteria, Pageable pageable) {
    return taskRepository.findAll(TaskSpec.getTaskSpecification(taskSearchCriteria), pageable);
  }

  public List<Task> getNonStructureTaskFacadesByLocationServerVersionAndPlan(UUID planIdentifier,
      List<UUID> locationIdentifiers, Long serverVersion) {
    return taskRepository.getNonStructureTaskFacadesByLocationServerVersionAndPlan(planIdentifier,
        locationIdentifiers, serverVersion);
  }

  public List<Task> getStructureTaskFacadesByLocationServerVersionAndPlan(UUID planIdentifier,
      List<UUID> locationIdentifiers, Long serverVersion) {
    return taskRepository.getStructureTaskFacadesByLocationServerVersionAndPlan(planIdentifier,
        locationIdentifiers, serverVersion);
  }

  public List<Task> getStructureForPeopleTaskFacadesByLocationServerVersionAndPlan(
      UUID planIdentifier,
      List<UUID> locationIdentifiers, Long serverVersion) {
    return taskRepository.getStructureForPeopleTaskFacadesByLocationServerVersionAndPlan(
        planIdentifier,
        locationIdentifiers, serverVersion);
  }

  public List<Task> getTasksAcrossPlansByBaseEntityIdentifiers(List<UUID> uuids) {
    return taskRepository.findAllByBaseEntityIdentifierIn(uuids);
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
  public List<Task> getTasksByIdentifierList(List<UUID> identifiers) {
    return taskRepository.findByIdentifierIn(identifiers);
  }

  public Task updateTask(UUID identifier, TaskUpdateRequest taskUpdateRequest) {

    Task taskToUpdate = getTaskByIdentifier(identifier);

    LookupTaskStatus lookupTaskStatus = lookupTaskStatusRepository.getById(
        taskUpdateRequest.getLookupTaskStatus());

    taskToUpdate.setLookupTaskStatus(lookupTaskStatus);
    taskToUpdate.setDescription(taskUpdateRequest.getDescription());
    taskToUpdate.setExecutionPeriodStart(taskUpdateRequest.getExecutionPeriodStart());
    taskToUpdate.setExecutionPeriodEnd(taskUpdateRequest.getExecutionPeriodEnd());
    taskToUpdate.setPriority(taskUpdateRequest.getPriority());

    return taskRepository.save(taskToUpdate);
  }

  public List<LookupTaskStatus> getAllTaskStatus() {
    return lookupTaskStatusRepository.findAll();
  }

  public void processPlanUpdateForTasks(UUID planIdentifier, String ownerId) {

    log.info("TASK_GENERATION Start generate tasks for Plan Id: {}", planIdentifier);
    Plan plan = planService.findPlanByIdentifier(planIdentifier);
    if (plan.getStatus().equals(PlanStatusEnum.ACTIVE)) {
      List<Goal> goals = goalService.getGoalsByPlanIdentifier(planIdentifier);

      List<ProcessTracker> processTrackerList = processTrackerService.findProcessTrackerByPlanIdentifierAndProcessTypeAndState(
          plan, ProcessType.PLAN_LOCATION_ASSIGNMENT, ProcessTrackerEnum.NEW);

      if (processTrackerList.size() >= 1) {
        ProcessTracker processTracker = processTrackerList.get(0);
        Set<String> excludedActionTitles = noTaskActionProperties.getActions().stream().map(
            ActionTitleEnum::getActionTitle).collect(
            Collectors.toSet());
        goals.stream().map(goal -> actionService.getActionsByGoalIdentifier(goal.getIdentifier()))
            .flatMap(Collection::stream)
            .filter(action -> !excludedActionTitles.contains(action.getTitle()))
            .forEach((action) -> processPlanUpdatePerActionForTasks(action, plan, ownerId,
                processTracker));

        processTrackerService.updateProcessTracker(processTracker.getIdentifier(),
            ProcessTrackerEnum.BUSY);

        if (processTrackerList.size() > 1) {
          log.debug(
              "cancelling remaining process trackers for this plan as there should only ever be one");
          IntStream.range(1, processTrackerList.size() - 1)
              .mapToObj(i -> processTrackerList.get(i).getIdentifier())
              .forEach(processTrackerIdentifier -> processTrackerService.updateProcessTracker(
                  processTrackerIdentifier,
                  ProcessTrackerEnum.CANCELLED));
        }
      }
      log.info("TASK_GENERATION Completed generating tasks for Plan Id: {}", planIdentifier);
    } else {
      log.info("TASK_GENERATION Not run as plan is not active: {}", planIdentifier);
    }

  }


  public void processPlanUpdatePerActionForTasks(Action action, Plan plan, String ownerId,
      ProcessTracker processTracker) {

    List<Condition> conditions = conditionService.getConditionsByActionIdentifier(
        action.getIdentifier());
    List<UUID> uuids = null;
    if (action.getTitle().equals(ActionTitleEnum.HABITAT_SURVEY.getActionTitle())) {
      uuids = getUuidsForTaskGenerationForHabitatSurvey(plan);
    } else if (action.getTitle().equals(ActionTitleEnum.LSM_HOUSEHOLD_SURVEY.getActionTitle())) {
      uuids = getUuidsForTaskGenerationForHouseholdSurvey(plan);
    } else if (
        plan.getInterventionType().getCode().equals(PlanInterventionTypeEnum.SURVEY.toString()) && (
            action.getTitle().equals(ActionTitleEnum.RCD.getActionTitle()) || action.getTitle()
                .equals(ActionTitleEnum.INDEX_CASE.getActionTitle()) || action.getTitle()
                .equals(ActionTitleEnum.SCREENING.getActionTitle()))) {
      uuids = new ArrayList<>();
    } else {
      uuids = getUuidsForTaskGeneration(action, plan, conditions);
    }
    processLocationListForTasks(action, plan, ownerId,
        processTracker, uuids, taskGenerationProperties.isGenerate(),
        taskGenerationProperties.isReactivate(), taskGenerationProperties.isCancel());

  }


  public Map<TaskGenerateRequestValidationStateEnum, List<UUID>> validateImportedLocationsForTaskGenerationDirect(
      List<UUID> suppliedLocationUuidList, Action action, Plan plan) {
    List<UUID> uuidsThatShouldBeInPlan = getUuidsForTaskGeneration(action, plan, null);

    List<UUID> existingTaskUuids = taskRepository.findUniqueByPlanAndActionidentifier(
            plan, action.getIdentifier())
        .stream().map(
            existingTask -> new TaskProjectionObj(existingTask.getIdentifier(),
                existingTask.getBaseEntityIdentifier()))
        .map(TaskProjectionObj::getBaseIdentifier)
        .map(UUID::fromString)
        .collect(
            Collectors.toList());

    List<UUID> alreadyExistingTasks = new ArrayList<>(uuidsThatShouldBeInPlan);
    alreadyExistingTasks.retainAll(existingTaskUuids);
    List<UUID> requestedButExisting = new ArrayList<>(suppliedLocationUuidList);
    requestedButExisting.retainAll(alreadyExistingTasks);

    List<UUID> alreadyExistingTasksCanGenerate = new ArrayList<>(uuidsThatShouldBeInPlan);
    alreadyExistingTasksCanGenerate.removeAll(existingTaskUuids);
    List<UUID> canGenerate = new ArrayList<>(suppliedLocationUuidList);
    canGenerate.retainAll(alreadyExistingTasksCanGenerate);

    List<UUID> shouldNotGenerateList = new ArrayList<>(suppliedLocationUuidList);
    shouldNotGenerateList.removeAll(uuidsThatShouldBeInPlan);

//    List<UUID> alreadyExistingTasksShouldBeCreated = new ArrayList<>(uuidsThatShouldBeInPlan);
//    alreadyExistingTasksShouldBeCreated.removeAll(existingTaskUuids);
//    alreadyExistingTasksShouldBeCreated.removeAll(suppliedLocationUuidList);

    return Map.of(TaskGenerateRequestValidationStateEnum.ALREADY_EXISTING, requestedButExisting,
        TaskGenerateRequestValidationStateEnum.CAN_GENERATE, canGenerate,
        TaskGenerateRequestValidationStateEnum.NOT_IN_PLAN, shouldNotGenerateList);
//    TaskGenerateRequestValidationStateEnum.SHOULD_GENERATE_BUT_NOT_REQUESTED,
//        alreadyExistingTasksShouldBeCreated

  }

  public Map<TaskGenerateRequestValidationStateEnum, List<UUID>> validateImportedLocationsForTaskGeneration(
      List<UUID> suppliedLocationUuidList, Action action, Plan plan) {
    List<UUID> uuidsThatShouldBeInPlan = getUuidsForTaskGeneration(action, plan, null);

    List<UUID> existingTaskUuids = taskRepository.findUniqueByPlanAndActionidentifier(
            plan, action.getIdentifier())
        .stream().map(
            existingTask -> new TaskProjectionObj(existingTask.getIdentifier(),
                existingTask.getBaseEntityIdentifier()))
        .map(TaskProjectionObj::getBaseIdentifier)
        .map(UUID::fromString)
        .collect(
            Collectors.toList());

    List<UUID> alreadyExistingTasks = new ArrayList<>(uuidsThatShouldBeInPlan);
    alreadyExistingTasks.retainAll(existingTaskUuids);
    List<UUID> requestedButExisting = new ArrayList<>(suppliedLocationUuidList);
    requestedButExisting.retainAll(alreadyExistingTasks);

    List<UUID> alreadyExistingTasksCanGenerate = new ArrayList<>(uuidsThatShouldBeInPlan);
    alreadyExistingTasksCanGenerate.removeAll(existingTaskUuids);
    List<UUID> canGenerate = new ArrayList<>(suppliedLocationUuidList);
    canGenerate.retainAll(alreadyExistingTasksCanGenerate);

    List<UUID> shouldNotGenerateList = new ArrayList<>(suppliedLocationUuidList);
    shouldNotGenerateList.removeAll(uuidsThatShouldBeInPlan);

    List<UUID> alreadyExistingTasksShouldBeCreated = new ArrayList<>(uuidsThatShouldBeInPlan);
    alreadyExistingTasksShouldBeCreated.removeAll(existingTaskUuids);
    alreadyExistingTasksShouldBeCreated.removeAll(suppliedLocationUuidList);

    return Map.of(TaskGenerateRequestValidationStateEnum.ALREADY_EXISTING, requestedButExisting,
        TaskGenerateRequestValidationStateEnum.CAN_GENERATE, canGenerate,
        TaskGenerateRequestValidationStateEnum.NOT_IN_PLAN, shouldNotGenerateList,
        TaskGenerateRequestValidationStateEnum.SHOULD_GENERATE_BUT_NOT_REQUESTED,
        alreadyExistingTasksShouldBeCreated);

  }

  public Pair<String, Map<TaskGenerateRequestValidationStateEnum, List<UUID>>> generateIndividualTask(
      UUID planIdentifier,
      UUID actionIdentifier, ListObj uuidsObj) {
    return generateIndividualTask(planIdentifier, actionIdentifier, uuidsObj, null);
  }

  public Pair<String, Map<TaskGenerateRequestValidationStateEnum, List<UUID>>> generateIndividualTask(
      UUID planIdentifier,
      UUID actionIdentifier, ListObj uuidsObj, UUID parentTaskIdentifier) {
    return generateIndividualTaskWithOwner(planIdentifier, actionIdentifier, uuidsObj,
        UserUtils.getCurrentPrincipleName(), parentTaskIdentifier);
  }

  public Pair<String, Map<TaskGenerateRequestValidationStateEnum, List<UUID>>> generateIndividualTaskWithOwner(
      UUID planIdentifier,
      UUID actionIdentifier, ListObj uuidsObj, String owner) {
    return generateIndividualTaskWithOwner(planIdentifier, actionIdentifier, uuidsObj, owner, null);
  }

  public Pair<String, Map<TaskGenerateRequestValidationStateEnum, List<UUID>>> generateIndividualTaskWithOwner(
      UUID planIdentifier,
      UUID actionIdentifier, ListObj uuidsObj, String owner, UUID parentTaskIdentifier) {
    Action action = actionService.getByIdentifier(actionIdentifier);

    Plan plan = action.getGoal().getPlan();

    List<UUID> uuids = uuidsObj.getUuids();

    Map<TaskGenerateRequestValidationStateEnum, List<UUID>> validatedMap = validateImportedLocationsForTaskGeneration(
        uuids, action, plan);

    if (validatedMap.containsKey(TaskGenerateRequestValidationStateEnum.ALREADY_EXISTING)) {
      if (validatedMap.get(TaskGenerateRequestValidationStateEnum.ALREADY_EXISTING) != null) {
        if (validatedMap.get(TaskGenerateRequestValidationStateEnum.ALREADY_EXISTING).size() > 0) {
          return Pair.of("No action taken as validation indicates supplied ids have tasks already",
              validatedMap);
        }
      }
    }

    if (validatedMap.containsKey(TaskGenerateRequestValidationStateEnum.NOT_IN_PLAN)) {
      if (validatedMap.get(TaskGenerateRequestValidationStateEnum.NOT_IN_PLAN) != null) {
        if (validatedMap.get(TaskGenerateRequestValidationStateEnum.NOT_IN_PLAN).size() > 0) {
          log.info("No action taken as validation indicates supplied not in plan assignment {}",
              validatedMap);
        }
      }
    }

    if (validatedMap.containsKey(TaskGenerateRequestValidationStateEnum.CAN_GENERATE)) {
      if (validatedMap.get(TaskGenerateRequestValidationStateEnum.CAN_GENERATE) != null) {
        if (validatedMap.get(TaskGenerateRequestValidationStateEnum.CAN_GENERATE).size() <= 0) {
          return Pair.of(
              "No action taken as validation indicates no eligible entities supplied to be generated",
              validatedMap);
        } else {

          ProcessTracker newProcessTracker = processTrackerService.createProcessTracker(
              UUID.randomUUID(),
              ProcessType.INDIVIDUAL_TASK_GENERATE, planIdentifier);

          log.debug("running process tracker");
          processLocationListForTasks(action, plan, owner,
              newProcessTracker,
              validatedMap.get(TaskGenerateRequestValidationStateEnum.CAN_GENERATE), true, false,
              false, parentTaskIdentifier);

          return Pair.of("No action taken as validation indicates entities should not be generated",
              validatedMap);
        }
      }
    }
    return Pair.of("No action taken as no validation object returned", validatedMap);

  }

  /**
   * Generates a single task against an {@code entity_data} record (for example an emanator).
   *
   * <p>Unlike {@link #generateIndividualTask}, the supplied identifier is NOT a location. It is an
   * {@code entity_data} identifier that is stored on the resulting task's
   * {@code baseEntityIdentifier} column. The task takes its locational grounding from the supplied
   * parent task's location, and is created with the default {@code "Not Visited"} business status.
   *
   * <p>Because the entity is not a location, the location-centric validations in
   * {@link #validateImportedLocationsForTaskGeneration} are not directly applicable. However the
   * entity_data record is linked to a location, so we reuse the plan/assignment validation against
   * that linked location to make sure the entity falls within the plan's eligible locations.
   */
  @Transactional
  public Pair<String, Map<TaskGenerateRequestValidationStateEnum, List<UUID>>> generateIndividualTaskForEntityData(
      UUID planIdentifier, UUID actionIdentifier, UUID entityDataIdentifier,
      UUID parentTaskIdentifier) {
    return generateIndividualTasksForEntityData(planIdentifier, actionIdentifier,
        Map.of(parentTaskIdentifier, List.of(entityDataIdentifier)));
  }

  /**
   * Generates many entity_data tasks in one request. The supplied map keys are parent task
   * identifiers and the values are the lists of {@code entity_data} identifiers to generate against
   * each parent. Every generated task stores its entity_data identifier in
   * {@code baseEntityIdentifier}, inherits the parent task's location, and is created with the
   * default {@code "Not Visited"} business status.
   *
   * <p>Task creation is routed through the {@code TASK_CANDIDATE_GENERATE} Kafka pipeline (process
   * tracker + task process stages) for consistency with {@link #generateIndividualTask}. The
   * returned validation map aggregates the per-entity outcomes across all parents.
   */
  @Transactional
  public Pair<String, Map<TaskGenerateRequestValidationStateEnum, List<UUID>>> generateIndividualTasksForEntityData(
      UUID planIdentifier, UUID actionIdentifier,
      Map<UUID, List<UUID>> parentToEntityDataIdentifiers) {
    return generateIndividualTasksForEntityDataWithOwner(planIdentifier, actionIdentifier,
        parentToEntityDataIdentifiers, UserUtils.getCurrentPrincipleName());
  }

  @Transactional
  public Pair<String, Map<TaskGenerateRequestValidationStateEnum, List<UUID>>> generateIndividualTasksForEntityDataWithOwner(
      UUID planIdentifier, UUID actionIdentifier,
      Map<UUID, List<UUID>> parentToEntityDataIdentifiers, String owner) {

    int parentCount = parentToEntityDataIdentifiers == null ? 0
        : parentToEntityDataIdentifiers.size();
    int entityCount = parentToEntityDataIdentifiers == null ? 0
        : parentToEntityDataIdentifiers.values().stream()
            .filter(list -> list != null)
            .mapToInt(List::size)
            .sum();
    log.info(
        "generateIndividualTasksForEntityData: ingestion start - plan={}, action={}, owner={}, parents={}, totalEntityData={}",
        planIdentifier, actionIdentifier, owner, parentCount, entityCount);

    Action action = actionService.getByIdentifier(actionIdentifier);
    Plan plan = action.getGoal().getPlan();

    List<UUID> alreadyExisting = new ArrayList<>();
    List<UUID> notInPlan = new ArrayList<>();
    List<UUID> canGenerate = new ArrayList<>();

    List<EntityDataTaskCandidate> candidates = new ArrayList<>();

    if (parentToEntityDataIdentifiers != null && !parentToEntityDataIdentifiers.isEmpty()) {

      // existing tasks already created for this plan+action, keyed on baseEntityIdentifier.
      // Use a Set for O(1) membership checks inside the loops below.
      Set<UUID> existingTaskBaseEntityIds = taskRepository.findUniqueByPlanAndActionidentifier(
              plan, action.getIdentifier())
          .stream()
          .map(TaskProjection::getBaseEntityIdentifier)
          .filter(id -> id != null)
          .map(UUID::fromString)
          .collect(Collectors.toSet());

      // locations eligible (assigned/filtered) for this plan+action
      Set<UUID> eligibleLocationUuids = new HashSet<>(getUuidsForTaskGeneration(action, plan, null));

      log.info(
          "generateIndividualTasksForEntityData: loaded {} existing task baseEntityIds and {} eligible plan locations",
          existingTaskBaseEntityIds.size(), eligibleLocationUuids.size());

      // Batch-fetch all parent tasks in one query (avoids an N+1 per parent) and index by id.
      Set<UUID> parentTaskIds = parentToEntityDataIdentifiers.keySet().stream()
          .filter(id -> id != null)
          .collect(Collectors.toSet());

      Map<UUID, Task> parentTasksById = taskRepository.findByIdentifierIn(
              new ArrayList<>(parentTaskIds))
          .stream()
          .collect(Collectors.toMap(Task::getIdentifier, task -> task));

      // Batch-fetch all referenced entity_data in one query (avoids an N+1 per entity) and index.
      Set<UUID> allEntityDataIds = parentToEntityDataIdentifiers.values().stream()
          .filter(list -> list != null)
          .flatMap(List::stream)
          .filter(id -> id != null)
          .collect(Collectors.toSet());

      Map<UUID, EntityData> entityDataById = entityDataRepository.findByIdentifierIn(allEntityDataIds)
          .stream()
          .collect(Collectors.toMap(EntityData::getIdentifier, entityData -> entityData));

      log.info(
          "generateIndividualTasksForEntityData: batch-fetched {}/{} parent tasks and {}/{} entity_data records",
          parentTasksById.size(), parentTaskIds.size(), entityDataById.size(),
          allEntityDataIds.size());

      for (Map.Entry<UUID, List<UUID>> entry : parentToEntityDataIdentifiers.entrySet()) {
        UUID parentTaskIdentifier = entry.getKey();
        List<UUID> entityDataIdentifiers = entry.getValue();

        if (parentTaskIdentifier == null || entityDataIdentifiers == null) {
          continue;
        }

        Task parentTask = parentTasksById.get(parentTaskIdentifier);
        if (parentTask == null) {
          throw new NotFoundException(Pair.of(Task.Fields.identifier, parentTaskIdentifier),
              Task.class);
        }

        Location parentLocation = parentTask.getLocation();
        if (parentLocation == null) {
          throw new IllegalArgumentException(
              "parent task " + parentTaskIdentifier + " has no location to inherit");
        }
        UUID parentLocationIdentifier = parentLocation.getIdentifier();

        for (UUID entityDataIdentifier : entityDataIdentifiers) {

          EntityData entityData = entityDataById.get(entityDataIdentifier);
          if (entityData == null) {
            throw new NotFoundException(
                Pair.of(EntityData.Fields.identifier, entityDataIdentifier), EntityData.class);
          }

          // Validation against the plan/assignment is done using the entity_data's linked location,
          // since the entity itself is not a location.
          boolean exists = existingTaskBaseEntityIds.contains(entityDataIdentifier);
          UUID entityLocation = entityData.getLocationIdentifier();
          boolean locationInPlan =
              entityLocation != null && eligibleLocationUuids.contains(entityLocation);

          if (exists) {
            alreadyExisting.add(entityDataIdentifier);
            log.debug(
                "generateIndividualTasksForEntityData: entity_data {} -> ALREADY_EXISTING (parent={})",
                entityDataIdentifier, parentTaskIdentifier);
          } else if (!locationInPlan) {
            notInPlan.add(entityDataIdentifier);
            log.debug(
                "generateIndividualTasksForEntityData: entity_data {} -> NOT_IN_PLAN (entityLocation={}, parent={})",
                entityDataIdentifier, entityLocation, parentTaskIdentifier);
          } else {
            canGenerate.add(entityDataIdentifier);
            candidates.add(new EntityDataTaskCandidate(entityDataIdentifier, parentTaskIdentifier,
                parentLocationIdentifier));
            log.debug(
                "generateIndividualTasksForEntityData: entity_data {} -> CAN_GENERATE (parent={}, inheritedLocation={})",
                entityDataIdentifier, parentTaskIdentifier, parentLocationIdentifier);
          }
        }
      }
    }

    Map<TaskGenerateRequestValidationStateEnum, List<UUID>> validatedMap = Map.of(
        TaskGenerateRequestValidationStateEnum.ALREADY_EXISTING, alreadyExisting,
        TaskGenerateRequestValidationStateEnum.CAN_GENERATE, canGenerate,
        TaskGenerateRequestValidationStateEnum.NOT_IN_PLAN, notInPlan);

    log.info(
        "generateIndividualTasksForEntityData: validation complete - canGenerate={}, alreadyExisting={}, notInPlan={}",
        canGenerate.size(), alreadyExisting.size(), notInPlan.size());

    if (candidates.isEmpty()) {
      log.info(
          "generateIndividualTasksForEntityData: no eligible entity_data to generate - no process tracker created");
      return Pair.of("No action taken as validation indicates no eligible entity_data to generate",
          validatedMap);
    }

    ProcessTracker newProcessTracker = processTrackerService.createProcessTracker(
        UUID.randomUUID(), ProcessType.INDIVIDUAL_TASK_GENERATE, planIdentifier);

    log.info(
        "generateIndividualTasksForEntityData: created processTracker {} for plan {}; submitting {} candidate(s)",
        newProcessTracker.getIdentifier(), planIdentifier, candidates.size());

    processEntityDataListForTasks(action, plan, owner, newProcessTracker, candidates);

    log.info(
        "generateIndividualTasksForEntityData: ingestion complete - submitted {} entity_data task(s) for generation under processTracker {}",
        candidates.size(), newProcessTracker.getIdentifier());

    return Pair.of("Submitted " + candidates.size() + " entity_data task(s) for generation",
        validatedMap);
  }

  /**
   * Builds {@link TaskProcessStage} rows for entity_data task candidates and submits them to the
   * {@code TASK_CANDIDATE_GENERATE} Kafka topic, mirroring {@link #processLocationListForTasks} but
   * carrying the inherited (parent) location and parent task on each candidate.
   */
  private void processEntityDataListForTasks(Action action, Plan plan, String ownerId,
      ProcessTracker processTracker, List<EntityDataTaskCandidate> candidates) {

    List<TaskProcessStage> taskCandidatesToProcess = candidates.stream()
        .map(candidate -> {
          TaskGen taskGen = TaskGen.forEntityData(candidate.getEntityDataIdentifier(),
              candidate.getLocationIdentifier());
          return getTaskProcessStage(processTracker, taskGen, candidate.getParentTaskIdentifier());
        })
        .collect(Collectors.toList());

    List<TaskProcessStage> taskProcessStages = taskProcessStageRepository.saveAll(
        taskCandidatesToProcess);

    List<UUID> stageIds = taskProcessStages.stream().map(TaskProcessStage::getIdentifier)
        .collect(Collectors.toList());

    // The process_tracker and task_process_stage rows are written in the current transaction. The
    // Kafka consumer reads them back by id, so publishing before commit causes a race where the
    // consumer cannot see the rows yet (observed: "processTracker ... NOT found"). Defer the Kafka
    // submission until after the transaction commits. If there is no active transaction, send now.
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      log.info(
          "processEntityDataListForTasks: saved {} entity_data task_process_stage rows {} for processTracker {}; deferring Kafka submission until after commit",
          taskProcessStages.size(), stageIds, processTracker.getIdentifier());

      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          log.info(
              "processEntityDataListForTasks: transaction committed - submitting {} entity_data stage(s) to Kafka for processTracker {}",
              taskProcessStages.size(), processTracker.getIdentifier());
          submitTaskCandidatesToKafka(action, plan, ownerId, taskProcessStages);
        }
      });
    } else {
      log.info(
          "processEntityDataListForTasks: saved {} entity_data task_process_stage rows {} for processTracker {}; no active transaction - submitting to Kafka immediately",
          taskProcessStages.size(), stageIds, processTracker.getIdentifier());
      submitTaskCandidatesToKafka(action, plan, ownerId, taskProcessStages);
    }
  }

  @lombok.Value
  private static class EntityDataTaskCandidate {

    UUID entityDataIdentifier;
    UUID parentTaskIdentifier;
    UUID locationIdentifier;
  }

  public void generateIndividualTaskWithOwnerDirect(
      UUID planIdentifier,
      UUID actionIdentifier, ListObj uuidsObj, String owner) {
    Action action = actionService.getByIdentifier(actionIdentifier);

    Plan plan = action.getGoal().getPlan();

    List<UUID> uuids = uuidsObj.getUuids();

    ProcessTracker newProcessTracker = processTrackerService.createProcessTracker(
        UUID.randomUUID(),
        ProcessType.INDIVIDUAL_TASK_GENERATE, planIdentifier);

    log.debug("running process tracker");
    processLocationListForTasks(action, plan, owner,
        newProcessTracker, uuids, true, false, false);
  }

  public void cancelIndividualTaskWithOwnerDirect(
      UUID planIdentifier,
      UUID actionIdentifier, ListObj uuidsObj, String owner) {
    Action action = actionService.getByIdentifier(actionIdentifier);

    Plan plan = action.getGoal().getPlan();

    List<UUID> uuids = uuidsObj.getUuids();

    ProcessTracker newProcessTracker = processTrackerService.createProcessTracker(
        UUID.randomUUID(),
        ProcessType.INDIVIDUAL_TASK_CANCEL, planIdentifier);

    log.debug("running process tracker");
    processLocationListForTasks(action, plan, owner,
        newProcessTracker, uuids, false, false, true);
  }

  public void processLocationListForTasks(Action action, Plan plan, String ownerId,
      ProcessTracker processTracker, List<UUID> uuids, boolean generate, boolean reactivate,
      boolean cancel) {
    processLocationListForTasks(action, plan, ownerId, processTracker, uuids, generate, reactivate,
        cancel, null);
  }

  public void processLocationListForTasks(Action action, Plan plan, String ownerId,
      ProcessTracker processTracker, List<UUID> uuids, boolean generate, boolean reactivate,
      boolean cancel, UUID parentTaskIdentifier) {

    List<TaskProjection> existingTasks = taskRepository.findUniqueByPlanAndActionidentifier(
        plan, action.getIdentifier());

    List<TaskProjectionObj> existingTaskObjs = existingTasks.stream().map(
        existingTask -> new TaskProjectionObj(existingTask.getIdentifier(),
            existingTask.getBaseEntityIdentifier())).collect(
        Collectors.toList());

    List<UUID> existingTaskUuids = existingTaskObjs.stream()
        .map(TaskProjectionObj::getBaseIdentifier)
        .map(UUID::fromString)
        .collect(
            Collectors.toList());

    List<TaskGen> tasksToProcess = new ArrayList<>();

    if (generate) {
      List<TaskGen> tasksToGenerate = buildGenerationTaskCandidateList(
          uuids, existingTaskUuids);

      tasksToProcess.addAll(tasksToGenerate);
    }

    if (reactivate) {
      List<TaskGen> tasksToReactivate = buildReactivationCandidateTaskList(
          uuids, existingTaskObjs, existingTaskUuids);

      tasksToProcess.addAll(tasksToReactivate);
    }

    if (cancel) {
      List<TaskGen> tasksToCancel = buildTaskCancellationCandidateList(
          uuids, existingTaskObjs, existingTaskUuids);

      tasksToProcess.addAll(tasksToCancel);
    }

    List<TaskProcessStage> taskCandidatesToProcess = tasksToProcess.stream()
        .map(taskGen -> getTaskProcessStage(processTracker, taskGen, parentTaskIdentifier))
        .collect(Collectors.toList());

    List<TaskProcessStage> taskProcessStages = taskProcessStageRepository.saveAll(
        taskCandidatesToProcess);

    log.debug("submitting taskProcessStages");
    submitTaskCandidatesToKafka(action, plan, ownerId, taskProcessStages);
  }

  private TaskProcessStage getTaskProcessStage(ProcessTracker processTracker, TaskGen taskGen,
      UUID parentTaskIdentifier) {
    TaskProcessStage taskGenerationStage = new TaskProcessStage();
    taskGenerationStage.setState(ProcessTrackerEnum.NEW);
    taskGenerationStage.setProcessTracker(processTracker);
    taskGenerationStage.setEntityStatus(EntityStatus.ACTIVE);
    taskGenerationStage.setTaskProcess(taskGen.getTaskProcessEnum());
    taskGenerationStage.setParentTaskIdentifier(parentTaskIdentifier);
    taskGenerationStage.setLocationIdentifier(taskGen.getLocationIdentifier());

    if (taskGen.getBaseEntityIdentifier() != null) {
      taskGenerationStage.setBaseEntityIdentifier(taskGen.getBaseEntityIdentifier());
    }
    if (taskGen.getIdentifier() != null) {
      taskGenerationStage.setTaskIdentifier(taskGen.getIdentifier());
    }
    return taskGenerationStage;
  }

  private List<TaskGen> buildGenerationTaskCandidateList(List<UUID> uuids,
      List<UUID> existingTaskUuids) {
    List<UUID> uuidToGenerate = new ArrayList<>(uuids);
    uuidToGenerate.removeAll(existingTaskUuids);

    List<TaskGen> tasksToGenerate = uuidToGenerate
        .stream().map(identifier -> new TaskGen(TaskProcessEnum.GENERATE, identifier))
        .collect(Collectors.toList());
    return tasksToGenerate;
  }

  private List<TaskGen> buildReactivationCandidateTaskList(List<UUID> uuids,
      List<TaskProjectionObj> existingTaskObjs,
      List<UUID> existingTaskUuids) {
    List<UUID> potentialUuidsToReactivate = new ArrayList<>(uuids);
    potentialUuidsToReactivate.retainAll(existingTaskUuids);

    List<TaskGen> tasksToReactivate = existingTaskObjs.stream().filter(
            existingTask -> potentialUuidsToReactivate.contains(
                UUID.fromString(existingTask.getBaseIdentifier())))
        .map(TaskProjectionObj::getIdentifier)
        .map(identifier -> new TaskGen(UUID.fromString(identifier), TaskProcessEnum.REACTIVATE))
        .collect(
            Collectors.toList());
    return tasksToReactivate;
  }

  private List<TaskGen> buildTaskCancellationCandidateList(List<UUID> uuids,
      List<TaskProjectionObj> existingTaskObjs,
      List<UUID> existingTaskUuids) {

    List<UUID> uuidsToCancel = new ArrayList<>(existingTaskUuids);
    uuidsToCancel.retainAll(uuids);

    List<TaskGen> tasksToCancel = existingTaskObjs.stream().filter(
            existingTask -> uuidsToCancel.contains(
                UUID.fromString(existingTask.getBaseIdentifier())))
        .map(TaskProjectionObj::getIdentifier)
        .map(identifier -> new TaskGen(UUID.fromString(identifier), TaskProcessEnum.CANCEL))
        .collect(
            Collectors.toList());

    return tasksToCancel;
  }

  private void submitTaskCandidatesToKafka(Action action, Plan plan, String ownerId,
      List<TaskProcessStage> taskProcessStages) {
    taskProcessStages.forEach(taskProcessStage -> {
          TaskProcessEvent taskProcessEvent = getTaskProcessEventObj(
              action, plan, ownerId, taskProcessStage);

          switch (taskProcessStage.getTaskProcess()) {
            case CANCEL:
              publisherService.send(
                  kafkaProperties.getTopicMap().get(KafkaConstants.TASK_CANDIDATE_CANCEL),
                  taskProcessEvent);
              break;
            case GENERATE:
              log.debug("submitting tasks {}", taskProcessEvent);
              publisherService.send(
                  kafkaProperties.getTopicMap().get(KafkaConstants.TASK_CANDIDATE_GENERATE),
                  taskProcessEvent);
              break;
            case REACTIVATE:
              publisherService.send(
                  kafkaProperties.getTopicMap().get(KafkaConstants.TASK_CANDIDATE_REACTIVATE),
                  taskProcessEvent);
              break;
          }
        }
    );
  }

  private TaskProcessEvent getTaskProcessEventObj(Action action, Plan plan, String ownerId,
      TaskProcessStage taskProcessStage) {
    TaskProcessEvent taskProcessEvent = TaskProcessEvent.builder()
        .baseEntityIdentifier(taskProcessStage.getBaseEntityIdentifier())
        .owner(ownerId)
        .taskProcessEnum(taskProcessStage.getTaskProcess())
        .actionEvent(ActionEvent.builder()
            .identifier(action.getIdentifier())
            .lookupEntityType(LookupEntityTypeEvent.builder()
                .code(action.getLookupEntityType().getCode())
                .build())
            .build())
        .planEvent(PlanEvent.builder()
            .identifier(plan.getIdentifier())
            .build())
        .state(taskProcessStage.getState())
        .processTracker(ProcessTrackerEvent.builder()
            .processTriggerIdentifier(
                taskProcessStage.getProcessTracker().getProcessTriggerIdentifier())
            .processType(taskProcessStage.getProcessTracker().getProcessType())
            .planIdentifier(taskProcessStage.getProcessTracker().getPlanIdentifier())
            .identifier(taskProcessStage.getProcessTracker().getIdentifier())
            .build()
        )
        .taskIdentifier(taskProcessStage.getTaskIdentifier())
        .parentTaskIdentifier(taskProcessStage.getParentTaskIdentifier())
        .locationIdentifier(taskProcessStage.getLocationIdentifier())
        .identifier(taskProcessStage.getIdentifier())
        .build();
    return taskProcessEvent;
  }


  @Transactional
  public void cancelTaskProcessStagesByProcessTrackerIdentifier(
      UUID processTrackerIdentifier) {
    Stream.of(ProcessTrackerEnum.BUSY, ProcessTrackerEnum.NEW)
        .forEach(trackerEnum -> taskProcessStageRepository.updateTaskGenerationState(
            ProcessTrackerEnum.CANCELLED,
            processTrackerIdentifier, trackerEnum)
        );
  }

  @PostConstruct
  private void getLookTaskStatuses() {
    cancelledLookupTaskStatus = lookupTaskStatusRepository.findByCode(
        TASK_STATUS_CANCELLED).orElseThrow(
        () -> new NotFoundException(Pair.of(LookupTaskStatus.Fields.code, TASK_STATUS_CANCELLED),
            LookupTaskStatus.class));

    readyLookupTaskStatus = lookupTaskStatusRepository.findByCode(
        TASK_STATUS_READY).orElseThrow(
        () -> new NotFoundException(Pair.of(LookupTaskStatus.Fields.code, TASK_STATUS_READY),
            LookupTaskStatus.class));

    completedLookupTaskStatus = lookupTaskStatusRepository.findByCode(
        TASK_STATUS_COMPLETED).orElseThrow(
        () -> new NotFoundException(Pair.of(LookupTaskStatus.Fields.code, TASK_STATUS_COMPLETED),
            LookupTaskStatus.class));
  }

  public Task generateTaskForTaskProcess(TaskProcessEvent taskProcessEvent) {
    Task task = null;
    Optional<TaskProcessStage> taskGenerationStageOptional = taskProcessStageRepository.findById(
        taskProcessEvent.getIdentifier());

    if (taskGenerationStageOptional.isEmpty()) {
      log.warn(
          "generateTaskForTaskProcess: task_process_stage {} NOT found (baseEntity={}, parentTask={}, location={}) - no task created. Likely the producer transaction had not committed when the Kafka message was consumed.",
          taskProcessEvent.getIdentifier(), taskProcessEvent.getBaseEntityIdentifier(),
          taskProcessEvent.getParentTaskIdentifier(), taskProcessEvent.getLocationIdentifier());
      return null;
    }

    if (!taskGenerationStageOptional.get().getState().equals(ProcessTrackerEnum.NEW)) {
      log.info(
          "generateTaskForTaskProcess: task_process_stage {} found but state is {} (not NEW) - no task created",
          taskProcessEvent.getIdentifier(), taskGenerationStageOptional.get().getState());
      return null;
    }

    {

      UUID uuid = taskProcessEvent.getBaseEntityIdentifier();

      Plan plan = planService.findPlanByIdentifier(
          taskProcessEvent.getPlanEvent().getIdentifier());

      Action action = actionService.getByIdentifier(
          taskProcessEvent.getActionEvent().getIdentifier());

      String owner = null;
      if (taskProcessEvent.getOwner() != null) {
        if (taskProcessEvent.getOwner().equals("kafka")) {
          owner = taskProcessEvent.getOwner();
        } else {
          User user = null;
          String userId = taskProcessEvent.getOwner();
          try {
            UUID userUUID = UUID.fromString(userId);
            user = userService.getByKeycloakId(userUUID);
          } catch (IllegalArgumentException e) {
            try {
              user = userService.getByUserName(userId);
            } catch (NotFoundException notFoundException) {
              user = null;
            }
          }
          if (user == null) {
            owner = userId;
          } else {
            owner = user.getUsername();
          }

        }
      } else {
        owner = "unknown";
      }
      boolean isEntityDataTask = taskProcessEvent.getLocationIdentifier() != null;
      log.info(
          "generateTaskForTaskProcess: creating {} task for stage {} (baseEntity={}, parentTask={}, location={}, owner={})",
          isEntityDataTask ? "entity_data" : "standard", taskProcessEvent.getIdentifier(), uuid,
          taskProcessEvent.getParentTaskIdentifier(), taskProcessEvent.getLocationIdentifier(),
          owner);

      task = createTaskObjectFromActionAndEntityId(action,
          uuid, plan, owner, taskProcessEvent.getParentTaskIdentifier(),
          taskProcessEvent.getLocationIdentifier());

      TaskProcessStage taskGenerationStage = taskGenerationStageOptional.get();
      taskGenerationStage.setState(ProcessTrackerEnum.DONE);
      taskProcessStageRepository.save(taskGenerationStage);

      log.info(
          "generateTaskForTaskProcess: created task {} and marked stage {} DONE",
          task != null ? task.getIdentifier() : null, taskProcessEvent.getIdentifier());

//      updateProcessTracker(taskProcessEvent);

    }

    return task;
  }

  public void updateProcessTracker(TaskProcessEvent taskProcessEvent) {

    Optional<TaskProcessStage> byId = taskProcessStageRepository.findById(
        taskProcessEvent.getIdentifier());

    if (byId.isPresent()) {
      TaskProcessStage taskProcessStage = byId.get();
      taskProcessStage.setState(ProcessTrackerEnum.DONE);
      taskProcessStageRepository.save(taskProcessStage);
    }

    int countOfTaskProcessStages = taskProcessStageRepository.countByProcessTracker_IdentifierAndStateNot(
        taskProcessEvent.getProcessTracker().getIdentifier(), ProcessTrackerEnum.DONE);

    if (countOfTaskProcessStages == 0) {
      processTrackerService.updateProcessTracker(
          taskProcessEvent.getProcessTracker().getIdentifier(),
          ProcessTrackerEnum.DONE);
    }
  }

  public void updateProcessTrackers() {

    List<TaskProcessStage> allByState = taskProcessStageRepository.findAllByState(
        ProcessTrackerEnum.NEW);

    allByState.forEach(taskProcessStage -> {
      int countOfTaskProcessStages = taskProcessStageRepository.countByProcessTracker_IdentifierAndStateNot(
          taskProcessStage.getProcessTracker().getIdentifier(), ProcessTrackerEnum.DONE);

      if (countOfTaskProcessStages == 0) {
        processTrackerService.updateProcessTracker(
            taskProcessStage.getProcessTracker().getIdentifier(),
            ProcessTrackerEnum.DONE);
      }
    });
  }

  private List<UUID> getUuidsForTaskGenerationForHabitatSurvey(Plan plan) {

    return planLocationsService.getPlanLocationsForHabitatSurvey(plan.getIdentifier(),
        LocationConstants.WATERBODY
    );
  }

  private List<UUID> getUuidsForTaskGenerationForHouseholdSurvey(Plan plan) {

    return planLocationsService.getPlanLocationsForHouseholdSurvey(plan.getIdentifier());
  }

  private List<UUID> getUuidsForTaskGeneration(Action action, Plan plan,
      List<Condition> conditions) {
    List<UUID> uuids = new ArrayList<>();

    if (conditions == null || conditions.isEmpty()) {
      try {
        uuids = entityFilterService.filterEntities(null, plan,
            plan.getLocationHierarchy().getIdentifier(), action);


      } catch (QueryGenerationException e) {
        log.error("unable to get tasks unconditionally for action: {}", action.getIdentifier(), e);
        e.printStackTrace();
      }
    } else {
      for (Condition condition : conditions) {
        Query query = ConditionQueryUtil.getQueryObject(condition.getQuery(),
            action.getLookupEntityType().getCode());

        try {
          List<UUID> filteredUUIDs = entityFilterService.filterEntities(query, plan,
              plan.getLocationHierarchy().getIdentifier(), action);

          uuids.addAll(filteredUUIDs);

        } catch (QueryGenerationException e) {
          log.error("unable to get tasks for action: {} condition: {}", action.getIdentifier(),
              condition.getIdentifier(), e);
          e.printStackTrace();
        }
      }
    }
    return uuids;
  }


  private Task createTaskObjectFromActionAndEntityId(Action action,
      UUID entityUUID, Plan plan, String owner) {
    return createTaskObjectFromActionAndEntityId(action, entityUUID, plan, owner, null, null);
  }

  private Task createTaskObjectFromActionAndEntityId(Action action,
      UUID entityUUID, Plan plan, String owner, UUID parentTaskIdentifier) {
    return createTaskObjectFromActionAndEntityId(action, entityUUID, plan, owner,
        parentTaskIdentifier, null);
  }

  /**
   * Creates and persists an individual task.
   *
   * <p>{@code entityUUID} is always stored as the task's {@code baseEntityIdentifier}. For location
   * and person actions it is also resolved against the respective store and attached.
   *
   * <p>When {@code locationIdentifier} is supplied the method treats this as an
   * <strong>entity_data</strong> task (for example an emanator): {@code entityUUID} is the
   * entity_data identifier (not a location/person), the task inherits the supplied location as its
   * locational grounding, and gets the default {@code "Not Visited"} business status. In that case
   * the location/person lookups are skipped because {@code entityUUID} does not resolve to either.
   */
  private Task createTaskObjectFromActionAndEntityId(Action action,
      UUID entityUUID, Plan plan, String owner, UUID parentTaskIdentifier,
      UUID locationIdentifier) {
    log.debug("TASK_GENERATION  create individual task for plan: {} and action: {}",
        plan.getIdentifier(), action.getIdentifier());

    boolean isEntityDataTask = locationIdentifier != null;

    boolean isActionForLocation = !isEntityDataTask && ActionUtils.isActionForLocation(action);

    boolean isActionForPerson = !isEntityDataTask && ActionUtils.isActionForPerson(action);

    Task task = Task.builder().lookupTaskStatus(
            lookupTaskStatusRepository.findByCode(TASK_STATUS_READY).orElseThrow(
                () -> new NotFoundException(Pair.of(LookupTaskStatus.Fields.code, TASK_STATUS_READY),
                    LookupTaskStatus.class))).priority(TaskPriorityEnum.ROUTINE)
        .description(action.getDescription())
        .authoredOn(LocalDateTime.now()).baseEntityIdentifier(entityUUID).action(action)
        .executionPeriodStart(action.getTimingPeriodStart())
        //TODO how to get this before save unless we do it on save of the task with kafka
        .identifier(UUID.randomUUID()).executionPeriodEnd(action.getTimingPeriodEnd()).plan(plan)
        .build();
    task.setBusinessStatus(businessStatusProperties.getDefaultBusinessStatus(action));

    task.setEntityStatus(EntityStatus.ACTIVE);

    if (parentTaskIdentifier != null) {
      Task parentTask = taskRepository.findById(parentTaskIdentifier).orElseThrow(
          () -> new NotFoundException(Pair.of(Task.Fields.identifier, parentTaskIdentifier),
              Task.class));
      task.setParentTaskIdentifier(parentTask);
    }

    if (isEntityDataTask) {
      // Entity_data (e.g. emanator) task: inherit the supplied (parent) location as the locational
      // grounding, but keep baseEntityIdentifier pointing at the entity_data id. Note that
      // Task.setLocation also overwrites baseEntityIdentifier, so we set it first then restore it.
      log.info(
          "TASK_GENERATION entity_data task: resolving inherited location {} for entity_data {}",
          locationIdentifier, entityUUID);
      Location location = locationService.findByIdentifier(locationIdentifier);
      task.setLocation(location);
      task.setBaseEntityIdentifier(entityUUID);
      task.setBusinessStatus(FormConstants.BusinessStatus.NOT_VISITED);
      log.info(
          "TASK_GENERATION entity_data task {}: location set to {}, baseEntityIdentifier set to {}, businessStatus=Not Visited",
          task.getIdentifier(), location.getIdentifier(), entityUUID);
    }
    if (isActionForLocation) {
      Location location = locationService.findByIdentifier(entityUUID);
      task.setLocation(location);
    }
    if (isActionForPerson) {
      Person person = personService.getPersonByIdentifier(entityUUID);
//      Set<UUID> collect = person.getLocations().stream().map(Location::getIdentifier)
//          .collect(Collectors.toSet());
//      Set<Location> locationsWithoutGeoJsonByIdentifierIn = locationService.findLocationsWithoutGeoJsonByIdentifierIn(
//          collect);
//      person.setLocations(locationsWithoutGeoJsonByIdentifierIn);
      task.setPerson(person);
    }

    Task savedTask = saveTaskAndBusinessState(task, owner);

    log.debug("TASK_GENERATION completed creating individual task for plan: {} and action: {}",
        plan.getIdentifier(), action.getIdentifier());

    return savedTask;
  }

  public Task reactivateTask(TaskProcessEvent taskProcessEvent) {

    Task savedTask = null;
    Optional<Task> taskOptional = taskRepository.findById(taskProcessEvent.getTaskIdentifier());

    if (taskOptional.isPresent()) {
      Task task = taskOptional.get();
      if (task.getLookupTaskStatus().getCode().equals(TASK_STATUS_CANCELLED)) {
        task.setLookupTaskStatus(this.readyLookupTaskStatus);
        savedTask = saveTaskAndBusinessState(task, taskProcessEvent.getOwner());
      }

    }
    updateProcessTracker(taskProcessEvent);
    return savedTask;
  }

  public Task cancelTask(TaskProcessEvent taskProcessEvent) {

    Task savedTask = null;
    Optional<Task> taskOptional = taskRepository.findById(taskProcessEvent.getTaskIdentifier());

    if (taskOptional.isPresent()) {
      Task task = taskOptional.get();
      if (!task.getLookupTaskStatus().getCode().equals(TASK_STATUS_CANCELLED)) {
        task.setLookupTaskStatus(this.cancelledLookupTaskStatus);
        savedTask = saveTaskAndBusinessState(task, taskProcessEvent.getOwner());
      }

    }
    updateProcessTracker(taskProcessEvent);
    return savedTask;
  }

  public Task saveTaskAndBusinessState(Task task, String owner) {

    log.trace("task: {} entity: {}", task.getIdentifier(),
        task.getBaseEntityIdentifier());
    TaskEvent taskEvent = TaskEventFactory.getTaskEventFromTask(task);
    taskEvent.setOwner(owner);
    task.setTaskFacade(taskEvent);

    if (task.getDescription() == null) {
      task.setDescription(task.getAction().getTitle());
    }

    Task savedTask = taskRepository.save(task);
    log.info("saveTaskAndBusinessState: persisted task {} (baseEntity={}, businessStatus={})",
        savedTask.getIdentifier(), savedTask.getBaseEntityIdentifier(),
        savedTask.getBusinessStatus());
    taskEvent.setIdentifier(savedTask.getIdentifier());
    taskEvent.setServerVersion(savedTask.getServerVersion());

    publisherService.send(kafkaProperties.getTopicMap().get(KafkaConstants.TASK), taskEvent);
    log.info("saveTaskAndBusinessState: published TASK event for task {}",
        savedTask.getIdentifier());

    return savedTask;
  }


  public List<String> getAllTasksNotSameAsTaskBusinessStateTracker() {
    return taskRepository.findTasksNotSameAsInTaskBusinessStateTracker();
  }

  public List<String> getAllTasksNotInTaskBusinessStateTracker() {
    return taskRepository.findTasksByNotPresentInTaskBusinessStateTracker();
  }


}
