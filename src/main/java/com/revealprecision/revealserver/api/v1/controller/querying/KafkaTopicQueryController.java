package com.revealprecision.revealserver.api.v1.controller.querying;

import com.revealprecision.revealserver.props.KafkaProperties;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kafka-config")
@Slf4j
@RequiredArgsConstructor
public class KafkaTopicQueryController {

  private final KafkaProperties kafkaProperties;

  @Value(value = "${kafka.bootstrapAddress}")
  private String kafkaBootStrapAddress;

  @Value(value = "${kafka.groupId}")
  private String kafkaGroupId;

  @GetMapping("/application-reset-topics")
  public String topics() {
    return kafkaProperties.getTopicMap().entrySet().stream()
        .filter(entry ->
            kafkaProperties.getResetTopicMap().get(entry.getKey()))
        .map(Entry::getValue)
        .collect(Collectors.joining(","));
  }


  @GetMapping("/all-topics")
  public String allTopics() {
    return String.join(",", kafkaProperties.getTopicMap().values());
  }

  @GetMapping("/group-id")
  public String kafkaGroupId() {
    return kafkaGroupId;
  }

  @GetMapping("/bootstrap-address")
  public String kafkaBootStrapAddress() {
    return kafkaBootStrapAddress;
  }

}
