package com.revealprecision.revealserver.api.v1.controller.querying;


import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.props.KafkaProperties;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@Profile("KafkaMessaging")
@RestController
@RequestMapping("/api/v1/kafkagroups")
@Slf4j
public class KafkaStatsController {

  private final KafkaAdmin kafkaAdmin;

  @Value(value = "${kafka.bootstrapAddress}") //TODO: test for multiple instances
  private String bootstrapAddress;

  @Value(value = "${kafka.groupId}") //TODO: test for multiple instances
  private String groupId;

  private final KafkaProperties kafkaProperties;

  @Qualifier("myKafkaAdminClient")
  private final AdminClient adminClient;

  @GetMapping("stats")
  private Map<String, Long> test() throws ExecutionException, InterruptedException {
    return analyzeLag();
  }

  private Map<TopicPartition, Long> getConsumerGrpOffsets()
      throws ExecutionException, InterruptedException {
    ListConsumerGroupOffsetsResult info = adminClient.listConsumerGroupOffsets(groupId);
    Map<TopicPartition, OffsetAndMetadata> topicPartitionOffsetAndMetadataMap = info.partitionsToOffsetAndMetadata()
        .get();

    Map<TopicPartition, Long> groupOffset = new HashMap<>();
    for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : topicPartitionOffsetAndMetadataMap.entrySet()) {
      TopicPartition key = entry.getKey();
      OffsetAndMetadata metadata = entry.getValue();
      groupOffset.putIfAbsent(new TopicPartition(key.topic(), key.partition()), metadata.offset());
    }
    return groupOffset;
  }

  private KafkaConsumer<String, String> getKafkaConsumer() {
    Properties properties = new Properties();
    properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class.getName());
    properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class.getName());
    return new KafkaConsumer<>(properties);
  }

  private Map<TopicPartition, Long> getProducerOffsets(
      Map<TopicPartition, Long> consumerGrpOffset) {
    List<TopicPartition> topicPartitions = new LinkedList<>();
    for (Map.Entry<TopicPartition, Long> entry : consumerGrpOffset.entrySet()) {
      TopicPartition key = entry.getKey();
      topicPartitions.add(new TopicPartition(key.topic(), key.partition()));
    }
    return getKafkaConsumer().endOffsets(topicPartitions);
  }

  private Map<TopicPartition, Long> computeLags(
      Map<TopicPartition, Long> consumerGrpOffsets,
      Map<TopicPartition, Long> producerOffsets) {
    Map<TopicPartition, Long> lags = new HashMap<>();
    for (Map.Entry<TopicPartition, Long> entry : consumerGrpOffsets.entrySet()) {
      Long producerOffset = producerOffsets.get(entry.getKey());
      Long consumerOffset = consumerGrpOffsets.get(entry.getKey());
      long lag = Math.abs(producerOffset - consumerOffset);
      lags.putIfAbsent(entry.getKey(), lag);
    }
    return lags;
  }

  public Map<String, Long> analyzeLag() throws ExecutionException, InterruptedException {
    Map<TopicPartition, Long> consumerGrpOffsets = getConsumerGrpOffsets();
    Map<TopicPartition, Long> producerOffsets = getProducerOffsets(consumerGrpOffsets);
    Map<TopicPartition, Long> lags = computeLags(consumerGrpOffsets, producerOffsets);
    Map<String, Long> collect = lags.entrySet().stream().collect(
        Collectors.groupingBy(topicPartitionLongEntry -> topicPartitionLongEntry.getKey().topic(),
            Collectors.summingLong(Entry::getValue)));
    Map<String, String> inverse = kafkaProperties.getTopicMap().entrySet().stream()
        .collect(Collectors.toMap(Entry::getValue,
            Entry::getKey));
    Map<String, Long> collect1 = collect.entrySet().stream().map(entry -> {
          log.debug("entry: {}", entry);
          if (inverse.containsKey(entry.getKey())){
            String s = inverse.get(entry.getKey());
            switch (s) {
              case KafkaConstants.TASK_CANDIDATE_GENERATE: {
                return new SimpleEntry<>("Task Generate", entry.getValue());
              }
              case KafkaConstants.TASK: {
                return new SimpleEntry<>("Task Processing", entry.getValue());
              }
              case KafkaConstants.LOCATIONS_IMPORTED: {
                return new SimpleEntry<>("Location Import", entry.getValue());
              }
              case KafkaConstants.LOCATION_METADATA_UPDATE: {
                return new SimpleEntry<>("Generate Hierarchy", entry.getValue());
              }
              case KafkaConstants.EVENT_AGGREGATION_LOCATION: {
                return new SimpleEntry<>("Metadata Import", entry.getValue());
              }
              default:
                return null;
            }
          } else {
            return null;
          }
        }).filter(Objects::nonNull)
        .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

    log.info("vals: {}", collect);

    for (Map.Entry<TopicPartition, Long> lagEntry : lags.entrySet()) {
      String topic = lagEntry.getKey().topic();
      int partition = lagEntry.getKey().partition();
      Long lag = lagEntry.getValue();
      log.info(" Lag for topic = {}, partition = {}, groupId = {} is {}",
          topic,
          partition,
          groupId,
          lag);
    }
    return collect1;
  }

}
