package com.revealprecision.revealserver.api;

import com.revealprecision.revealserver.persistence.domain.Task;
import com.revealprecision.revealserver.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/")
public class TaskController {
	private final TaskService taskService;

	@Autowired
	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}
	@Operation(summary = "Search for Tasks",
			description = "Search for Tasks",
			tags = { "Task" }
	)
	@ResponseStatus(HttpStatus.OK)
	@GetMapping(value = "/task",
			produces = "application/json"
	)
	public Page<Task> getTasks(
			@Parameter(description = "Plan identifier of the task") @RequestParam(required = false) String planIdentifier,
			@Parameter(description = "Page number to return") @RequestParam(defaultValue = "0", required = false) Integer pageNumber,
			@Parameter(description = "Number of records per page") @RequestParam(defaultValue = "50", required = false) Integer pageSize) {
		return taskService.getTasks(pageNumber,pageSize);
	}

	@Operation(summary = "Fetch a Task by identifier",
			description = "Fetch a Task by identifier",
			tags = { "Task" }
	)
	@ResponseStatus(HttpStatus.OK)
	@GetMapping(value = "/task/{identifier}",
			produces = "application/json"
	)
	public Task getTaskByIdentifier(@Parameter(description = "Task identifier") @PathVariable("identifier") String taskIdentifier){
		return taskService.getTaskByIdentifier(taskIdentifier);
	}

	@Operation(summary = "Create a task",
			description = "Create a Task",
			tags = { "Task" }
	)
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping(value = "/task", consumes = "application/json", produces = "application/json")
	public Task createTask(@Validated @RequestBody Task task) {
		return taskService.createTask(task);
	}

	@Operation(summary = "Create a task",
			description = "Create a Task",
			tags = { "Task" }
	)
	@ResponseStatus(HttpStatus.CREATED)
	@PutMapping(value = "/task/{identifier}", consumes = "application/json", produces = "application/json")
	public Task createTask(@Parameter(description = "GUID task identifier") @PathVariable("identifier") String identifier, @Validated @RequestBody Task task) {
		return taskService.updateTask(identifier,task);
	}
}