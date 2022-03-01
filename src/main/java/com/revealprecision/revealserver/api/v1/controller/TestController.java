package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.exceptions.QueryGenerationException;
import com.revealprecision.revealserver.service.TaskService;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
@RequestMapping("/test")
public class TestController {

  @Autowired
  TaskService taskService;

  @Autowired
  JdbcTemplate jdbcTemplate;

  @GetMapping("/generateTasks")
  public void generateTasks() throws QueryGenerationException {

    List<UUID> identifier = jdbcTemplate
        .query("SELECT identifier from plan where name = 'OnePlan'",
            (rs, rowNum) -> (UUID) rs.getObject("identifier"));

    taskService.generateTasksByPlanId(identifier.get(0));

  }

  @GetMapping("/updateTasks")
  public void updateTasks() {

    List<UUID> identifier = jdbcTemplate
        .query("SELECT identifier from plan where name = 'OnePlan'",
            (rs, rowNum) -> (UUID) rs.getObject("identifier"));

    taskService.updateTasks(identifier.get(0));

  }
}
