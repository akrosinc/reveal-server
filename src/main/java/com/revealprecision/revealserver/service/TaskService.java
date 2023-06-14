package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.TaskEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.TaskCreateRequest;
import com.revealprecision.revealserver.api.v1.dto.request.TaskUpdateRequest;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.enums.ActionTitleEnum;
import com.revealprecision.revealserver.enums.EntityStatus;
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
import com.revealprecision.revealserver.persistence.repository.LookupTaskStatusRepository;
import com.revealprecision.revealserver.persistence.repository.TaskProcessStageRepository;
import com.revealprecision.revealserver.persistence.repository.TaskRepository;
import com.revealprecision.revealserver.persistence.specification.TaskSpec;
import com.revealprecision.revealserver.props.BusinessStatusProperties;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.props.TaskGenerationProperties;
import com.revealprecision.revealserver.service.models.TaskSearchCriteria;
import com.revealprecision.revealserver.util.ActionUtils;
import com.revealprecision.revealserver.util.ConditionQueryUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        goals.stream().map(goal -> actionService.getActionsByGoalIdentifier(goal.getIdentifier()))
            .flatMap(Collection::stream)
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
    } else {
      uuids = getUuidsForTaskGeneration(action, plan, conditions);
    }
    processLocationListForTasks(action, plan, ownerId,
        processTracker, uuids, taskGenerationProperties.isGenerate(),
        taskGenerationProperties.isReactivate(), taskGenerationProperties.isCancel());

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

  public void processLocationListForTasks(Action action, Plan plan, String ownerId,
      ProcessTracker processTracker, List<UUID> uuids, boolean generate, boolean reactivate,
      boolean cancel) {

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
        .map(taskGen -> getTaskProcessStage(processTracker, taskGen)).collect(Collectors.toList());

    List<TaskProcessStage> taskProcessStages = taskProcessStageRepository.saveAll(
        taskCandidatesToProcess);

    submitTaskCandidatesToKafka(action, plan, ownerId, taskProcessStages);
  }

  private TaskProcessStage getTaskProcessStage(ProcessTracker processTracker, TaskGen taskGen) {
    TaskProcessStage taskGenerationStage = new TaskProcessStage();
    taskGenerationStage.setState(ProcessTrackerEnum.NEW);
    taskGenerationStage.setProcessTracker(processTracker);
    taskGenerationStage.setEntityStatus(EntityStatus.ACTIVE);
    taskGenerationStage.setTaskProcess(taskGen.getTaskProcessEnum());

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
    uuidsToCancel.removeAll(uuids);

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

    if (taskGenerationStageOptional.isPresent() && taskGenerationStageOptional.get().getState()
        .equals(ProcessTrackerEnum.NEW)) {

      UUID uuid = taskProcessEvent.getBaseEntityIdentifier();

      Plan plan = planService.findPlanByIdentifier(
          taskProcessEvent.getPlanEvent().getIdentifier());

      Action action = actionService.getByIdentifier(
          taskProcessEvent.getActionEvent().getIdentifier());

      String owner = null;
      if (taskProcessEvent.getOwner() != null) {
        User user = userService.getByKeycloakId(UUID.fromString(taskProcessEvent.getOwner()));
        owner = user.getUsername();
      }
      task = createTaskObjectFromActionAndEntityId(action,
          uuid, plan, owner);

      TaskProcessStage taskGenerationStage = taskGenerationStageOptional.get();
      taskGenerationStage.setState(ProcessTrackerEnum.DONE);
      taskProcessStageRepository.save(taskGenerationStage);

      updateProcessTracker(taskProcessEvent);

    }

    return task;
  }

  private void updateProcessTracker(TaskProcessEvent taskProcessEvent) {

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
    log.debug("TASK_GENERATION  create individual task for plan: {} and action: {}",
        plan.getIdentifier(), action.getIdentifier());

    boolean isActionForLocation = ActionUtils.isActionForLocation(action);

    boolean isActionForPerson = ActionUtils.isActionForPerson(action);

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

    Task savedTask = taskRepository.save(task);
    taskEvent.setIdentifier(savedTask.getIdentifier());
    taskEvent.setServerVersion(savedTask.getServerVersion());

    if (savedTask.getLookupTaskStatus().getCode().equals(TASK_STATUS_CANCELLED)) {
      businessStatusService.deactivateBusinessStatus(savedTask);
    } else {
      businessStatusService.activateBusinessStatus(savedTask);
    }

    publisherService.send(kafkaProperties.getTopicMap().get(KafkaConstants.TASK), taskEvent);

    return savedTask;
  }


  public List<String> getAllTasksNotSameAsTaskBusinessStateTracker(){
    return taskRepository.findTasksNotSameAsInTaskBusinessStateTracker();
  }

  public List<String> getAllTasksNotInTaskBusinessStateTracker(){
    return taskRepository.findTasksByNotPresentInTaskBusinessStateTracker();
  }


}