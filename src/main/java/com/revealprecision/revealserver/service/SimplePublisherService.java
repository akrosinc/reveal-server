package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.messaging.message.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("!KafkaMessaging")
@Service
@Slf4j
public class SimplePublisherService implements PublisherService{


  @Override
  public void send(String topic, Message message) {
    log.info("Mock sending {} - {}", topic, message);
  }

  @Override
  public void send(String topic, String key, Message message) {
    log.info("Mock sending {} - {} - {}", topic,key, message);
  }
}
