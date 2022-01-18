package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.persistence.repository.TaskRepository;
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
        return save;
    }

    public Task getTaskByIdentifier(UUID identifier) {
        return taskRepository.findByIdentifier(identifier);
    }
    public Task updateTask(String identifier, Task task) {
        return null;
    }
}