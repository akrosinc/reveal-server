package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.messaging.message.PlanUpdateMessage;
import com.revealprecision.revealserver.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanUpdateListener extends Listener {

  private final TaskService taskService;

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('PLAN_UPDATE')}", groupId = "reveal_server_group")
  public void listenGroupFoo(PlanUpdateMessage message) {
    //TODO: make this traceable - i.e. the application should know when task generation starts / ends
    log.info("Received Message in group foo: {}" , message.toString());
    init();
    taskService.generateTasksByPlanId(message.getPlanIdentifier(),message.getOwnerId());
    taskService.cancelApplicableTasksByPlanId(message.getPlanIdentifier(),message.getOwnerId());
  }
}
