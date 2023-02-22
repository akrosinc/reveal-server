package com.revealprecision.revealserver.config;

import com.revealprecision.revealserver.messaging.message.Message;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

  @Value(value = "${kafka.bootstrapAddress}")
  private String bootstrapAddress;

  @Value(value = "${kafka.groupId}")
  private String groupId;

  @Bean
  public ConsumerFactory<String, Message> consumerFactory() { //TODO: consider serialization/deserialization failures
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
    props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
        "com.revealprecision.revealserver.messaging.message.Message");
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    props.put(JsonDeserializer.KEY_DEFAULT_TYPE, String.class);

    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean
  public ConsumerFactory<String, Message> batchConsumerFactory() { //TODO: consider serialization/deserialization failures
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
    props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
        "com.revealprecision.revealserver.messaging.message.Message");
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.revealprecision");
    props.put(JsonDeserializer.KEY_DEFAULT_TYPE, String.class);
    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 30);
    props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,String.valueOf(10*1000));
    props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG,String.valueOf(10*1000));
    props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG,300000);
    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Message>
  kafkaListenerContainerFactory() {

    ConcurrentKafkaListenerContainerFactory<String, Message> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    return factory;
  }

  @Bean
  @Qualifier("batch")
  public ConcurrentKafkaListenerContainerFactory<String, Message>
  kafkaBatchListenerContainerFactory() {

    ConcurrentKafkaListenerContainerFactory<String, Message> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(batchConsumerFactory());
    factory.setBatchListener(true);
    return factory;
  }
}
