package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.messaging.TopicConstants;
import com.revealprecision.revealserver.messaging.message.PlanLocationAssignMessage;
import com.revealprecision.revealserver.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanLocationListener extends Listener {

  private final TaskService taskService;

  @KafkaListener(topics = TopicConstants.PLAN_LOCATION, groupId = "reveal_server_group")
  public void listenGroupFoo(PlanLocationAssignMessage message) {
    System.out.println("Received Message in group foo: " + message.toString());
    init();
    taskService.generateTasksByPlanId(message.getPlanIdentifier());
    taskService.cancelApplicableTasksByPlanId(message.getPlanIdentifier());
  }
}
