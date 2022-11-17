package com.revealprecision.revealserver.api.v1.controller.querying;

import com.revealprecision.revealserver.enums.ProcessType;
import com.revealprecision.revealserver.enums.TaskGenerateRequestValidationStateEnum;
import com.revealprecision.revealserver.persistence.domain.Action;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.ProcessTracker;
import com.revealprecision.revealserver.service.ActionService;
import com.revealprecision.revealserver.service.ProcessTrackerService;
import com.revealprecision.revealserver.service.TaskService;
import com.revealprecision.revealserver.util.UserUtils;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
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

    Action action = actionService.getByIdentifier(actionIdentifier);

    Plan plan = action.getGoal().getPlan();

    List<UUID> uuids = uuidsObj.getUuids();

    Map<TaskGenerateRequestValidationStateEnum, List<UUID>> validatedMap = taskService.validateImportedLocationsForTaskGeneration(
        uuids, action, plan);

    if (validatedMap.containsKey(TaskGenerateRequestValidationStateEnum.ALREADY_EXISTING)){
      if (validatedMap.get(TaskGenerateRequestValidationStateEnum.ALREADY_EXISTING) != null) {
        if (validatedMap.get(TaskGenerateRequestValidationStateEnum.ALREADY_EXISTING).size() > 0){
          return Pair.of("No action taken as validation indicates supplied ids have tasks already",validatedMap);
        }
      }
    }

    if (validatedMap.containsKey(TaskGenerateRequestValidationStateEnum.NOT_IN_PLAN)){
      if (validatedMap.get(TaskGenerateRequestValidationStateEnum.NOT_IN_PLAN) != null) {
        if (validatedMap.get(TaskGenerateRequestValidationStateEnum.NOT_IN_PLAN).size() > 0){
          return Pair.of("No action taken as validation indicates supplied not in plan assignment",validatedMap);
        }
      }
    }


    if (validatedMap.containsKey(TaskGenerateRequestValidationStateEnum.CAN_GENERATE)){
      if (validatedMap.get(TaskGenerateRequestValidationStateEnum.CAN_GENERATE) != null){
        if (validatedMap.get(TaskGenerateRequestValidationStateEnum.CAN_GENERATE).size() <= 0){
          return Pair.of("No action taken as validation indicates no eligible entities supplied to be generated",validatedMap);
        }else{

          ProcessTracker newProcessTracker = processTrackerService.createProcessTracker(
              UUID.randomUUID(),
              ProcessType.INDIVIDUAL_TASK_GENERATE, planIdentifier);

          taskService.processLocationListForTasks(action,plan,UserUtils.getCurrentPrincipleName(),newProcessTracker,uuids,true,false,false);

          return Pair.of("No action taken as validation indicates entities should not be generated",validatedMap);
        }
      }
    }
    return Pair.of("No action taken as no validation object returned",validatedMap);
  }

  @Data
  public static class ListObj implements Serializable {
    List<UUID> uuids;
  }

}
