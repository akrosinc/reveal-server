package com.revealprecision.revealserver.config;

import com.revealprecision.revealserver.props.KafkaProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaAdmin.NewTopics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

@EnableKafka
@Configuration
@Slf4j
@RequiredArgsConstructor
@Profile("KafkaMessaging")
public class KafkaConfig {

  private final KafkaProperties kafkaProperties;

  @Value(value = "${kafka.bootstrapAddress}") //TODO: test for multiple instances
  private String bootstrapAddress;

  @Bean
  public KafkaAdmin kafkaAdmin() {
    Map<String, Object> configs = new HashMap<>();
    configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    return new KafkaAdmin(configs);
  }

  @Bean
  public NewTopics setup() {

    List<NewTopic> topicsThatExpireMessages = kafkaProperties.getTopicNames().stream()
        .filter(topic -> kafkaProperties.getTopicConfigRetention().containsKey(topic))
        .map(topic -> {
          TopicBuilder topicBuilder = TopicBuilder.name(topic)
              .config(TopicConfig.RETENTION_MS_CONFIG,
                  String.valueOf(kafkaProperties.getTopicConfigRetention().get(topic)));
          if (kafkaProperties.getTopicPartitions().containsKey(topic)) {
            topicBuilder.partitions(kafkaProperties.getTopicPartitions().get(topic));
          }
          return topicBuilder.build();
        })
        .collect(Collectors.toList());

    List<NewTopic> topicsThatDoNotExpireMessages = kafkaProperties.getTopicMap().entrySet().stream()
        .filter(topic -> !kafkaProperties.getTopicConfigRetention().containsKey(topic.getKey()))
        .map(topic -> {
          TopicBuilder topicBuilder = TopicBuilder.name(topic.getValue());
          if (kafkaProperties.getTopicPartitions().containsKey(topic.getKey())) {
            topicBuilder.partitions(kafkaProperties.getTopicPartitions().get(topic.getKey()));
          }
          return topicBuilder.build();
        })
        .collect(Collectors.toList());

    List<NewTopic> newTopics = new ArrayList<>();
    newTopics.addAll(topicsThatExpireMessages);
    newTopics.addAll(topicsThatDoNotExpireMessages);

    return new NewTopics(newTopics.toArray(NewTopic[]::new));
  }


  @Bean
  public DeadLetterPublishingRecoverer recoverer(@Autowired KafkaTemplate kafkaTemplate) {
    return new DeadLetterPublishingRecoverer(kafkaTemplate,
        (record, ex) -> new TopicPartition("recovererDLQ", -1));
  }
}
