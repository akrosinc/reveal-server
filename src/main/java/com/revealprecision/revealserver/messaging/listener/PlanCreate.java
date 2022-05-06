package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.messaging.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanCreate {

  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('PLAN_CREATE')}", groupId = "reveal_server_group")
  public void listenGroupFoo(Message message) {
    System.out.println("Received Message in group foo: " + message.toString());
  }

}
