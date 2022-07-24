package com.revealprecision.revealserver.service;

import static com.revealprecision.revealserver.constants.LocationConstants.STRUCTURE;

import com.revealprecision.revealserver.api.v1.dto.factory.TaskEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.TaskCreateRequest;
import com.revealprecision.revealserver.api.v1.dto.request.TaskUpdateRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.PlanStatusEnum;
import com.revealprecision.revealserver.enums.ProcessTrackerEnum;
import com.revealprecision.revealserver.enums.ProcessType;
import com.revealprecision.revealserver.enums.TaskPriorityEnum;
import com.revealprecision.revealserver.enums.TaskProcessEnum;
import com.revealprecision.revealserver.exceptions.DuplicateTaskCreationException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.QueryGenerationException;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.messaging.TaskEventFactory;
import com.revealprecision.revealserver.messaging.dto.TaskGen;
import com.revealprecision.revealserver.messaging.message.ActionEvent;
import com.revealprecision.revealserver.messaging.message.LookupEntityTypeEvent;
import com.revealprecision.revealserver.messaging.message.Message;
import com.revealprecision.revealserver.messaging.message.PlanEvent;
import com.revealprecision.revealserver.messaging.message.ProcessTrackerEvent;
import com.revealprecision.revealserver.messaging.message.TaskEvent;
import com.revealprecision.revealserver.messaging.message.TaskProcessEvent;
import com.revealprecision.revealserver.messaging.message.TaskProjectionObj;
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
import com.revealprecision.revealserver.persistence.domain.ProcessTracker;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.domain.Task.Fields;
import com.revealprecision.revealserver.persistence.domain.TaskProcessStage;
import com.revealprecision.revealserver.persistence.domain.actioncondition.Query;
import com.revealprecision.revealserver.persistence.projection.TaskProjection;
import com.revealprecision.revealserver.persistence.repository.LookupTaskStatusRepository;
import com.revealprecision.revealserver.persistence.repository.TaskProcessStageRepository;
import com.revealprecision.revealserver.persistence.repository.TaskRepository;
import com.revealprecision.revealserver.persistence.specification.TaskSpec;
import com.revealprecision.revealserver.props.BusinessStatusProperties;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.models.TaskSearchCriteria;
import com.revealprecision.revealserver.util.ActionUtils;
import com.revealprecision.revealserver.util.ConditionQueryUtil;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
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
  private final TaskProcessStageRepository taskProcessStageRepository;
  private final ProcessTrackerService processTrackerService;


  private LookupTaskStatus cancelledLookupTaskStatus;
  private LookupTaskStatus readyLookupTaskStatus;

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
      KafkaProperties kafkaProperties,
      TaskProcessStageRepository taskProcessStageRepository,
      ProcessTrackerService processTrackerService) {
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
    this.taskProcessStageRepository = taskProcessStageRepository;
    this.processTrackerService = processTrackerService;
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
            .forEach((action) -> processPlanUpdatePerActionForTasks(action, plan, ownerId, processTracker));

        processTrackerService.updateProcessTracker(processTracker.getIdentifier(),
            ProcessTrackerEnum.BUSY);

        if (processTrackerList.size() > 1) {
          log.debug("cancelling remaining process trackers for this plan as there should only ever be one");
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

  public void processPlanUpdatePerActionForTasks(Action action, Plan plan, String ownerId, ProcessTracker processTracker) {

    List<Condition> conditions = conditionService.getConditionsByActionIdentifier(
        action.getIdentifier());

    List<UUID> uuids = getUuidsForTaskGeneration(action, plan, conditions);

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

    List<UUID> potentialUuidsToReactivate = new ArrayList<>(uuids);
    potentialUuidsToReactivate.retainAll(existingTaskUuids);

    List<TaskGen> tasksToReactivate = existingTaskObjs.stream().filter(
            existingTask -> potentialUuidsToReactivate.contains(
                UUID.fromString(existingTask.getBaseIdentifier())))
        .map(TaskProjectionObj::getIdentifier)
        .map(identifier -> new TaskGen(UUID.fromString(identifier), TaskProcessEnum.REACTIVATE))
        .collect(
            Collectors.toList());

    List<UUID> uuidsToCancel = new ArrayList<>(existingTaskUuids);
    uuidsToCancel.removeAll(uuids);

    List<TaskGen> tasksToCancel = existingTaskObjs.stream().filter(
            existingTask -> uuidsToCancel.contains(
                UUID.fromString(existingTask.getBaseIdentifier())))
        .map(TaskProjectionObj::getIdentifier)
        .map(identifier -> new TaskGen(UUID.fromString(identifier), TaskProcessEnum.CANCEL))
        .collect(
            Collectors.toList());

    List<UUID> uuidToGenerate = new ArrayList<>(uuids);
    uuidToGenerate.removeAll(existingTaskUuids);

    List<TaskGen> tasksToGenerate = uuidToGenerate
        .stream().map(identifier -> new TaskGen(TaskProcessEnum.GENERATE, identifier))
        .collect(Collectors.toList());

    List<TaskGen> tasksToProcess = new ArrayList<>();
    tasksToProcess.addAll(tasksToGenerate);
    tasksToProcess.addAll(tasksToCancel);
    tasksToProcess.addAll(tasksToReactivate);

    log.debug("tasksToGenerate: {} tasksToCancel: {} tasksToReactivate: {} existingTasks: {}",
        tasksToGenerate.size(), tasksToCancel.size(), tasksToReactivate.size(), existingTaskUuids.size());


    List<TaskProcessStage> collect = tasksToProcess.stream()
        .map(taskGen -> {
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
        }).collect(Collectors.toList());

    List<TaskProcessStage> taskProcessStages = taskProcessStageRepository.saveAll(
        collect);

      taskProcessStages.forEach(taskProcessStage -> {
        // Proceed with caution here as new updates / removals to the object will prevent rewind of the kafka listener application.
        // In the event of new data being introduced, ensure that null pointers are catered in the kafka listener
        // application if the event comes through, and it does not have the new fields populated
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
            kafkaTemplate.send(kafkaProperties.getTopicMap().get(KafkaConstants.TASK_CANDIDATE),
                taskProcessEvent);
          }

    );
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
  }

  public Task generateTaskForTaskProcess(TaskProcessEvent taskProcessEvent) throws IOException {
    Task taskObjs = null;
    Optional<TaskProcessStage> taskGenerationStageOptional = taskProcessStageRepository.findById(
        taskProcessEvent.getIdentifier());

    if (taskGenerationStageOptional.isPresent() && taskGenerationStageOptional.get().getState()
        .equals(ProcessTrackerEnum.NEW)) {

      UUID uuid = taskProcessEvent.getBaseEntityIdentifier();

      Plan plan = planService.getPlanByIdentifier(
          taskProcessEvent.getPlanEvent().getIdentifier());

      Action action = actionService.getByIdentifier(
          taskProcessEvent.getActionEvent().getIdentifier());

      String ownerId = taskProcessEvent.getOwner();
      taskObjs = createTaskObjectFromActionAndEntityId(action,
          uuid, plan, ownerId);

      TaskProcessStage taskGenerationStage = taskGenerationStageOptional.get();
      taskGenerationStage.setState(ProcessTrackerEnum.DONE);
      taskProcessStageRepository.save(taskGenerationStage);

      updateProcessTracker(taskProcessEvent);

    }

    return taskObjs;
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


  private List<UUID> getUuidsForTaskGeneration(Action action, Plan plan,
      List<Condition> conditions) {
    List<UUID> uuids = new ArrayList<>();

    if (conditions == null || conditions.isEmpty()) {
      try {
        uuids = entityFilterService.filterEntities(null, plan,
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
          List<UUID> filteredUUIDs = entityFilterService.filterEntities(query, plan,
              plan.getLocationHierarchy().getIdentifier(), action);

          uuids.addAll(filteredUUIDs);

        } catch (QueryGenerationException e) {
          log.error("unable to get tasks for action: {} condition: {}", action.getIdentifier(),
              condition.getIdentifier());
          e.printStackTrace();
        }
      }
    }
    return uuids;
  }


  private Task createTaskObjectFromActionAndEntityId(Action action,
      UUID entityUUID, Plan plan, String ownerId) throws IOException {
    log.debug("TASK_GENERATION  create individual task for plan: {} and action: {}",
        plan.getIdentifier(), action.getIdentifier());

    boolean isActionForLocation = ActionUtils.isActionForLocation(action);

    boolean isActionForPerson = ActionUtils.isActionForPerson(action);

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

    Task savedTask = saveTaskAndBusinessState(task, ownerId);

    log.debug("TASK_GENERATION completed creating individual task for plan: {} and action: {}",
        plan.getIdentifier(), action.getIdentifier());

    return savedTask;
  }

  public Task reactivateTask(TaskProcessEvent taskProcessEvent) throws IOException {

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

  public Task cancelTask(TaskProcessEvent taskProcessEvent) throws IOException {

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

  public Task saveTaskAndBusinessState(Task task, String ownerId) throws IOException {

    Task savedTask = taskRepository.save(task);

    if (savedTask.getLookupTaskStatus().getCode().equals(TASK_STATUS_CANCELLED)) {
      businessStatusService.deactivateBusinessStatus(savedTask);
    } else {
      businessStatusService.setBusinessStatus(savedTask,
          businessStatusProperties.getDefaultLocationBusinessStatus());
    }

    log.trace("task: {} entity: {}", savedTask.getIdentifier(),
        savedTask.getBaseEntityIdentifier());
    TaskEvent taskEvent = TaskEventFactory.getTaskEventFromTask(savedTask);
    taskEvent.setOwnerId(ownerId);
    kafkaTemplate.send(kafkaProperties.getTopicMap().get(KafkaConstants.TASK), taskEvent);

    return savedTask;
  }


  public void updateOrganizationsAndLocationsForTasksByPlanIdentifier(UUID planIdentifier) {
    log.info("TASK_ASSIGN Start update tasks for Plan Id: {}", planIdentifier);

    List<Task> tasksByPlan = taskRepository.findTasksByPlan_Identifier(planIdentifier);
//TODO - resolve the dependency injection issue so that @Lazy is not used in this class
    Plan plan = planService.findPlanByIdentifier(planIdentifier);

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


  private Task markUnassignedTaskAsCancelled(Plan plan, LookupTaskStatus cancelledLookupTaskStatus,
      Task task) {
    log.debug("TASK_CANCEL Start cancellation for task: {}",
        task.getIdentifier());
    Action action = task.getAction();

    log.debug("TASK_CANCEL getting plan locations: {}", task.getIdentifier());

    List<PlanLocations> planLocationsForLocation = getPlanLocationsForLocation(plan, task, action);

    List<PlanLocations> planLocationsForPerson = getPlanLocationsForPerson(plan, task, action);
    log.debug("TASK_CANCEL got plan locations: {}", task.getIdentifier());

    if (!task.getLookupTaskStatus().getCode().equals(cancelledLookupTaskStatus.getCode())) {
      if (planLocationsForLocation.isEmpty() && planLocationsForPerson.isEmpty()) {
        task.setLookupTaskStatus(cancelledLookupTaskStatus);
        log.debug("TASK_CANCEL task cancelled: {}", task.getIdentifier());
      }
    } else {
      log.debug("TASK_CANCEL task not cancelled: {}", task.getIdentifier());
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

      return locationService.getLocationsByPeople(task.getPerson().getIdentifier()).stream()
          .flatMap(location -> {
            if (location.getGeographicLevel().getName().equals(STRUCTURE)) {
              Location parentLocation = locationService.getLocationParent(location,
                  plan.getLocationHierarchy());
              return planLocationsService.getPlanLocationsByPlanAndLocationIdentifier(
                  parentLocation.getIdentifier(), plan.getIdentifier()).stream();
            } else {

              return planLocationsService.getPlanLocationsByLocationIdentifierList(
                  List.of(location.getIdentifier())).stream();
            }
          }).collect(Collectors.toList());

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
      Plan plan = planService.findPlanByIdentifier(planIdentifier);
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