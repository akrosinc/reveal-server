package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.messaging.message.PlanLocationAssignMessage;
import com.revealprecision.revealserver.service.TaskService;
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

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('PLAN_LOCATION_ASSIGNED')}", groupId = "reveal_server_group")
  public void listenGroupFoo(PlanLocationAssignMessage message) {
    log.info("Received Message in group foo: {}" , message.toString());
    init();
    taskService.generateTasksByPlanId(UUID.fromString(message.getPlanIdentifier()),message.getOwnerId());
    taskService.cancelApplicableTasksByPlanId(UUID.fromString(message.getPlanIdentifier()),message.getOwnerId());
  }
}
