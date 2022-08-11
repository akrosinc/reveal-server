package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagEventFactory;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.messaging.message.LocationFormDataSumAggregateEvent;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.EntityTagService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.MetadataService;
import com.revealprecision.revealserver.service.PlanService;
import com.revealprecision.revealserver.util.UserUtils;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataAggregateListener extends Listener {

  ReadOnlyKeyValueStore<String, LocationFormDataSumAggregateEvent> locationFormDataIntegerSumOrAverage;
  private final MetadataService metadataService;
  private final EntityTagService entityTagService;
  private final PlanService planService;
  private final LocationService locationService;
  private final StreamsBuilderFactoryBean getKafkaStreams;
  private final KafkaProperties kafkaProperties;


  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('METADATA_AGGREGATE')}", groupId = "reveal_server_group")
  public void listenGroupFoo(ConsumerRecord<String, LocationFormDataSumAggregateEvent> message) {
    init();
    locationFormDataIntegerSumOrAverage = getKafkaStreams.getKafkaStreams().store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.locationFormDataIntegerSumOrAverage),
            QueryableStoreTypes.keyValueStore()));
    log.info("Received Message k: {}  v: {}", message.key(), message.value());
    String k = message.key();
    LocationFormDataSumAggregateEvent aggMessage = message.value();

    String[] keySplit = k.split("_");

    String planId = keySplit[0];
    Plan plan = null;
    if (planId!=null && !planId.equals("plan") ){
      plan = planService.findNullablePlanByIdentifier(UUID.fromString(planId));
    }

    String entityParentIdentifier = keySplit[2];

    Location location = locationService.findByIdentifier(
        UUID.fromString(entityParentIdentifier));

    UUID entityTagIdentifier = aggMessage.getEntityTagIdentifier();
    LocationFormDataSumAggregateEvent locationFormDataSumAggregateEvent = locationFormDataIntegerSumOrAverage.get(
        k);

    if (locationFormDataSumAggregateEvent != null) {

      Optional<EntityTag> entityTagById = entityTagService.findEntityTagById(entityTagIdentifier);

      if (entityTagById.isPresent()) {
        EntityTag entityTag = entityTagById.get();

        if (locationFormDataSumAggregateEvent.getSum() != null) {

          Optional<EntityTag> entityTagByTagName = entityTagService.getEntityTagByTagName(
              entityTag.getTag() + "-sum");

          if (entityTagByTagName.isPresent()) {
            EntityTagEvent entityTagEvent = EntityTagEventFactory.getEntityTagEvent(entityTagByTagName.get());
            if (entityTagEvent.isAggregate()) {
              metadataService.updateMetaData(UUID.fromString(entityParentIdentifier), locationFormDataSumAggregateEvent.getSum(),
                  plan, null,
                  UserUtils.getCurrentPrincipleName(), entityTag.getValueType(), entityTagEvent,
                  "aggregate", location, "aggregate",
                  Location.class, k + "-sum", null);
            }
          }
        }
      }
    }


  }
}
