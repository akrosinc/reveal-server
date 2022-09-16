package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.enums.ProcessTrackerEnum;
import com.revealprecision.revealserver.enums.ProcessType;
import com.revealprecision.revealserver.messaging.message.PlanLocationAssignMessage;
import com.revealprecision.revealserver.persistence.domain.ProcessTracker;
import com.revealprecision.revealserver.service.AssignedStructureService;
import com.revealprecision.revealserver.service.ProcessTrackerService;
import com.revealprecision.revealserver.service.TaskService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanLocationListener extends Listener {

  private final TaskService taskService;
  private final ProcessTrackerService processTrackerService;
  private final AssignedStructureService assignedStructureService;



  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('PLAN_LOCATION_ASSIGNED')}", groupId = "reveal_server_group")
  public void listenGroupFoo(PlanLocationAssignMessage message) {
    log.info("Received Message in group foo: {}" , message.toString());
    init();

    int deleteByPlan = message.getDeleteByPlan();
    int deleteByPlanAndLocation = message.getDeleteByPlanAndLocation();
    int saveAll = message.getSaveAll();
    updateProcessTracker(deleteByPlan,saveAll,deleteByPlanAndLocation, message.getPlanIdentifier());

    taskService.processPlanUpdateForTasks(message.getPlanIdentifier(),message.getOwnerId());

    assignedStructureService.refreshAssignedStructureCountsMaterializedView();
  }

  private void updateProcessTracker(int deleteByPlan, int saveAll,
      int deleteByPlanAndLocation, UUID planIdentifier){
    log.trace("deleteByPlan: {} saveAll: {} deleteByPlanAndLocation: {} ", deleteByPlan, saveAll,
        deleteByPlanAndLocation);

    if (deleteByPlan > 0 || saveAll > 0 || deleteByPlanAndLocation > 0) {


      List<ProcessTracker> processTrackers = processTrackerService.findProcessTrackerByPlanAndProcessType(
          planIdentifier,
          ProcessType.PLAN_LOCATION_ASSIGNMENT);

      for (ProcessTracker processTracker : processTrackers) {
        if (processTracker.getState().equals(ProcessTrackerEnum.NEW) || processTracker.getState().equals(ProcessTrackerEnum.BUSY)) {
          processTrackerService.updateProcessTracker(processTracker.getIdentifier(),
              ProcessTrackerEnum.CANCELLED);
            taskService.cancelTaskProcessStagesByProcessTrackerIdentifier(processTracker.getIdentifier());
        }
      }

      ProcessTracker newProcessTracker = processTrackerService.createProcessTracker(
          UUID.randomUUID(),
          ProcessType.PLAN_LOCATION_ASSIGNMENT, planIdentifier);
      log.trace("processTracker identifier: {}", newProcessTracker.getIdentifier());
    }

  }
}
