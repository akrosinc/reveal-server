package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.messaging.message.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Profile("KafkaMessaging")
@RequiredArgsConstructor
@Service
public class KafkaPublisherService implements PublisherService{

  private final KafkaTemplate<String, Message> stringListKafkaTemplate;

  @Override
  public void send(String topic, Message message) {
    stringListKafkaTemplate.send(topic,message);
  }

  @Override
  public void send(String topic, String key, Message message) {
    stringListKafkaTemplate.send(topic,key,message);
  }
}
