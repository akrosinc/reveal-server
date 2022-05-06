package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.messaging.TopicConstants;
import com.revealprecision.revealserver.messaging.message.PlanUpdateMessage;
import com.revealprecision.revealserver.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanUpdateListener extends Listener {

  private final TaskService taskService;

  @KafkaListener(topics = TopicConstants.PLAN_UPDATE, groupId = "reveal_server_group")
  public void listenGroupFoo(PlanUpdateMessage message) {
    //TODO: make this traceable - i.e. the application should know when task generation starts / ends
    System.out.println("Received Message in group foo: " + message.toString());
    init();
    taskService.generateTasksByPlanId(message.getPlanIdentifier());
    taskService.cancelApplicableTasksByPlanId(message.getPlanIdentifier());
  }
}
