package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.messaging.Message;
import com.revealprecision.revealserver.messaging.TopicConstants;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PlanCreate {

  @KafkaListener(topics = TopicConstants.PLAN_CREATE, groupId = "reveal_server_group")
  public void listenGroupFoo(Message message) {
    System.out.println("Received Message in group foo: " + message.toString());
  }

}
