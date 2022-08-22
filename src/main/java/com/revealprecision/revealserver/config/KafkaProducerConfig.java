package com.revealprecision.revealserver.config;

import com.revealprecision.revealserver.messaging.message.Message;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaProducerConfig<T extends Message> {

  @Value(value = "${kafka.bootstrapAddress}")
  private String bootstrapAddress;

  @Bean
  public ProducerFactory<String, T> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
        bootstrapAddress);
    configProps.put(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
        StringSerializer.class);
    configProps.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        JsonSerializer.class);
//    configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS,false);
    return new DefaultKafkaProducerFactory<>(configProps);
  }

  @Bean
  public KafkaTemplate<String, T> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }
}