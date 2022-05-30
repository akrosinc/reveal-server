package com.revealprecision.revealserver.config;

import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.COMMIT_INTERVAL_MS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG;
import static org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME;

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
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaAdmin.NewTopics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.streams.RecoveringDeserializationExceptionHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerde;

@EnableKafka
@Configuration
@EnableKafkaStreams
@Slf4j
@RequiredArgsConstructor
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
        .map(topic -> TopicBuilder.name(topic).config(TopicConfig.RETENTION_MS_CONFIG,
            String.valueOf(kafkaProperties.getTopicConfigRetention().get(topic))).build())
        .collect(Collectors.toList());

    List<NewTopic> topicsThatDoNotExpireMessages = kafkaProperties.getTopicNames().stream()
        .filter(topic -> !kafkaProperties.getTopicConfigRetention().containsKey(topic))
        .map(topic -> TopicBuilder.name(topic)
            .build()
        ).collect(Collectors.toList());

    List<NewTopic> newTopics = new ArrayList<>();
    newTopics.addAll(topicsThatExpireMessages);
    newTopics.addAll(topicsThatDoNotExpireMessages);

    return new NewTopics(newTopics.toArray(NewTopic[]::new));
  }

  @Bean(name = DEFAULT_STREAMS_CONFIG_BEAN_NAME)
  KafkaStreamsConfiguration kStreamsConfig(
      @Autowired DeadLetterPublishingRecoverer deadLetterPublishingRecoverer) {
    Map<String, Object> props = commonprops(deadLetterPublishingRecoverer);
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, kafkaProperties.getApplicationId());
    props.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class);
    props.put(COMMIT_INTERVAL_MS_CONFIG, "5000");
    return new KafkaStreamsConfiguration(props);
  }

  private Map<String, Object> commonprops(
      DeadLetterPublishingRecoverer deadLetterPublishingRecoverer) {
    Map<String, Object> props = new HashMap<>();
    props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    props.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

    props.put(JsonDeserializer.TRUSTED_PACKAGES,
        "com.revealprecision.revealserver.messaging.message");
    props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
        RecoveringDeserializationExceptionHandler.class);
    props.put(RecoveringDeserializationExceptionHandler.KSTREAM_DESERIALIZATION_RECOVERER,
        deadLetterPublishingRecoverer);
    return props;
  }

  @Bean
  public DeadLetterPublishingRecoverer recoverer(@Autowired KafkaTemplate kafkaTemplate) {
    return new DeadLetterPublishingRecoverer(kafkaTemplate,
        (record, ex) -> new TopicPartition("recovererDLQ", -1));
  }
}
