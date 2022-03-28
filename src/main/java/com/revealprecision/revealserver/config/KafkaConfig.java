package com.revealprecision.revealserver.config;

import com.revealprecision.revealserver.messaging.TopicConstants;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaAdmin;

@EnableKafka
@Configuration
public class KafkaConfig {


  @Value(value = "${kafka.bootstrapAddress}")
  private String bootstrapAddress;

  @Bean
  public KafkaAdmin kafkaAdmin() {
    Map<String, Object> configs = new HashMap<>();
    configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    return new KafkaAdmin(configs);
  }

  @Bean
  public NewTopic planCreateTopic() {
    return new NewTopic(TopicConstants.PLAN_CREATE, 1, (short) 1);
  }

  @Bean
  public NewTopic planUpdateTopic() {
    return new NewTopic(TopicConstants.PLAN_UPDATE, 1, (short) 1);
  }
}
