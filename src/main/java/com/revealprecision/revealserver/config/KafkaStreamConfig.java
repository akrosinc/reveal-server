package com.revealprecision.revealserver.config;

import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.COMMIT_INTERVAL_MS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG;
import static org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME;

import com.revealprecision.revealserver.props.KafkaProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.streams.RecoveringDeserializationExceptionHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
//@EnableKafkaStreams
@Slf4j
@RequiredArgsConstructor

public class KafkaStreamConfig {

  private final KafkaProperties kafkaProperties;

  @Value(value = "${kafka.bootstrapAddress}") //TODO: test for multiple instances
  private String bootstrapAddress;

  @Bean(name = DEFAULT_STREAMS_CONFIG_BEAN_NAME)
  KafkaStreamsConfiguration kStreamsConfig(
      @Autowired DeadLetterPublishingRecoverer deadLetterPublishingRecoverer) {
    Map<String, Object> props = commonprops(deadLetterPublishingRecoverer);
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, kafkaProperties.getApplicationId());
    props.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class);
    props.put(COMMIT_INTERVAL_MS_CONFIG, "5000");
    props.put(MAX_POLL_INTERVAL_MS_CONFIG, String.valueOf(45 * 60 * 1000));
    return new KafkaStreamsConfiguration(props);
  }

  private Map<String, Object> commonprops(
      DeadLetterPublishingRecoverer deadLetterPublishingRecoverer) {
    Map<String, Object> props = new HashMap<>();
    props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    props.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

    props.put(JsonDeserializer.TRUSTED_PACKAGES,
        "com.revealprecision.revealserver.messaging.*");
    props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
        RecoveringDeserializationExceptionHandler.class);
    props.put(RecoveringDeserializationExceptionHandler.KSTREAM_DESERIALIZATION_RECOVERER,
        deadLetterPublishingRecoverer);
    return props;
  }
}
