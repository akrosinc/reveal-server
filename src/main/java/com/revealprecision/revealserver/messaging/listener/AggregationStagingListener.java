package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.messaging.message.LocationIdEvent;
import com.revealprecision.revealserver.persistence.domain.AggregationStaging;
import com.revealprecision.revealserver.persistence.repository.AggregationStagingRepository;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("KafkaMessaging & Simulation & (Listening | location-id-listener))")
public class AggregationStagingListener extends Listener {

  private final AggregationStagingRepository aggregationStagingRepository;


  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('AGGREGATION_STAGING')}", groupId = "reveal_server_group")
  public void processMessage(LocationIdEvent message) {
    log.debug("Received message {}", message);

    aggregationStagingRepository.saveAll(
        message.getUuids().stream().map(identifier ->
                getAggregationStaging(message.getHierarchyIdentifier(), identifier,message.getNodeOrder())
            ).collect(Collectors.toList()));

  }

  private AggregationStaging getAggregationStaging(UUID hierarchyIdentifier, UUID identifier, String nodeOrder) {
    return AggregationStaging.builder()
        .locationIdentifier(identifier)
        .hierarchyIdentifier(hierarchyIdentifier)
        .nodeOrder(nodeOrder)
        .build();
  }



}
