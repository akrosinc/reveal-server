package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.enums.TaskStatusEnum;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.repository.TaskRepository;
import org.apache.catalina.mapper.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TaskService {
    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);
    private final TaskRepository taskRepository;
    private final ProducerService producerService;
    private final ObjectMapper objectMapper;

    @Autowired
    public TaskService(TaskRepository taskRepository, ProducerService producerService, ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.producerService = producerService;
        this.objectMapper = objectMapper;
    }
    public Page<Task> getTasks(Integer pageNumber, Integer pageSize){
        return taskRepository.findAll(PageRequest.of(pageNumber,pageSize));
    }

    public Task createTask(Task task) {
        Task save = taskRepository.save(task);
        logger.info("Task saved to database as {}", task);

//        try {
//            producerService.sendMessage(objectMapper.writeValueAsString(task));
//        } catch (JsonProcessingException e) {
//            logger.debug("Task not mapped {}", e.getMessage());
//        }
        return save;
    }

    public Task getTaskByIdentifier(UUID identifier) {
        return taskRepository.findByIdentifier(identifier);
    }

//    public Page<Task> getTaskByPlanIdentifier(UUID planIdentifier, Integer pageNumber, Integer pageSize) {
//        return taskRepository.findByPlanIdentifier(planIdentifier, pageNumber, pageSize);
//    }

    public Task updateTask(String identifier, Task task) {
        return null;
    }

//    public Page<Task> findTasksByCriteria(UUID planIdentifier, TaskStatusEnum status, Integer pageNumber, Integer pageSize) {
//
//        if (planIdentifier != null) {
//            return this.getTaskByPlanIdentifier(planIdentifier, pageNumber, pageSize);
//        }
//        else {
//            return this.getTasks(pageNumber, pageSize);
//        }
//    }
}