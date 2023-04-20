package com.revealprecision.revealserver.props;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("kafkaConfigProperties")
@ConfigurationProperties(prefix = "kafka.config")
@Setter
@Getter
public class KafkaProperties {

  private String suffix;

  private List<String> topicNames = new ArrayList<>();

  private Map<String, String> topicMap = new HashMap<>();
  private Map<String, Boolean> resetTopicMap = new HashMap<>();
  private Map<String, String> topicConfigRetention = new HashMap<>();
  private Map<String, Integer> topicPartitions = new HashMap<>();

  private List<String> storeNames = new ArrayList<>();

  private Map<String, String> storeMap = new HashMap<>();

  private String applicationId;
}
