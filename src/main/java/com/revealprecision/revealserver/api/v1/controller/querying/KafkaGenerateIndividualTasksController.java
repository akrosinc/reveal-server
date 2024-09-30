package com.revealprecision.revealserver.api.v1.controller.querying;

import com.revealprecision.revealserver.enums.TaskGenerateRequestValidationStateEnum;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.service.ActionService;
import com.revealprecision.revealserver.service.ProcessTrackerService;
import com.revealprecision.revealserver.service.TaskService;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kafka-config")
@Slf4j
@RequiredArgsConstructor
public class KafkaGenerateIndividualTasksController {

  private final ProcessTrackerService processTrackerService;
  private final TaskService taskService;
  private final ActionService actionService;

  @PostMapping("/validate-individual-tasks")
  public Map<TaskGenerateRequestValidationStateEnum, List<UUID>> validate(@RequestParam("planIdentifier") UUID planIdentifier,
      @RequestParam("actionIdentifier") UUID actionIdentifier, @RequestBody ListObj uuidsObj) {

    Action action = actionService.getByIdentifier(actionIdentifier);

    Plan plan = action.getGoal().getPlan();
    List<UUID> uuids = uuidsObj.getUuids();
    return taskService.validateImportedLocationsForTaskGeneration(
        uuids, action, plan);
  }

  @PostMapping("/generate-individual-tasks")
  public Pair<String,Map<TaskGenerateRequestValidationStateEnum, List<UUID>>> generate(@RequestParam("planIdentifier") UUID planIdentifier,
      @RequestParam("actionIdentifier") UUID actionIdentifier, @RequestBody ListObj uuidsObj) {

    return taskService.generateIndividualTask(planIdentifier, actionIdentifier,uuidsObj);
  }

  @Data
  public static class ListObj implements Serializable {
    List<UUID> uuids;
  }

}
