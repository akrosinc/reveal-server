package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.factory.TaskEntityFactory;
import com.revealprecision.revealserver.api.v1.dto.request.TaskRequest;
import com.revealprecision.revealserver.api.v1.dto.request.TaskUpdateRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.DuplicateTaskCreationException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Form;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.domain.Task.Fields;
import com.revealprecision.revealserver.persistence.repository.TaskRepository;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.data.util.Pair;

@Service
@Slf4j
public class TaskService {

  private final TaskRepository taskRepository;
  private final ObjectMapper objectMapper;
  private final FormService formService;
  private final PlanService planService;
  private final ActionService actionService;

  @Autowired
  public TaskService(TaskRepository taskRepository, ProducerService producerService,
      ObjectMapper objectMapper, FormService formService, PlanService planService, ActionService actionService) {
    this.taskRepository = taskRepository;
    this.objectMapper = objectMapper;
    this.formService = formService;
    this.planService = planService;
    this.actionService = actionService;
  }

  public Page<Task> getTasks(Pageable pageable) {
    return taskRepository.findAll(pageable);
  }

  public Task createTask(TaskRequest taskRequest) {

    Form form = formService.findById(taskRequest.getIntstantiatesUri());
    Plan plan = planService.getPlanByIdentifier(taskRequest.getPlanIdentifier());

    Action action = actionService.getByIdentifier(UUID.fromString(taskRequest.getCode()));
    //Action exists

    if (!taskRepository.findTaskByCode(action.getIdentifier().toString()).isEmpty()){
      throw new DuplicateTaskCreationException("Task for action id".concat(taskRequest.getCode()).concat(" already exists"));
    }

    Task task = TaskEntityFactory.entityFromRequestObj(taskRequest, form, plan);
    task.setEntityStatus(EntityStatus.ACTIVE);
    return taskRepository.save(task);
  }

  public Task getTaskByIdentifier(UUID identifier) {
    return taskRepository.findByIdentifier(identifier).orElseThrow(()->new NotFoundException(Pair.of(
        Fields.identifier,identifier),Task.class));
  }

  public Task updateTask(UUID identifier, TaskUpdateRequest taskUpdateRequest) {

    Task taskToUpdate = getTaskByIdentifier(identifier);
    Form form = formService.findById(taskUpdateRequest.getIntstantiatesUri());

    taskToUpdate.setBusinessStatus(taskUpdateRequest.getBusinessStatus());
    taskToUpdate.setLastModified(LocalDateTime.now());
    taskToUpdate.setDescription(taskUpdateRequest.getDescription());
    taskToUpdate.setExecutionPeriodStart(Date.from(taskUpdateRequest.getExecutionPeriodStart().atStartOfDay(
        ZoneId.systemDefault()).toInstant()));
    taskToUpdate.setExecutionPeriodEnd(Date.from(taskUpdateRequest.getExecutionPeriodEnd().atStartOfDay(
        ZoneId.systemDefault()).toInstant()));
    taskToUpdate.setPriority(taskUpdateRequest.getPriority());
    taskToUpdate.setInstantiatesUriForm(form);

    return taskRepository.save(taskToUpdate);
  }
}