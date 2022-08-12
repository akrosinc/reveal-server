package com.revealprecision.revealserver.messaging.listener;

import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagEventFactory;
import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;
import com.revealprecision.revealserver.constants.EntityTagDataTypes;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.enums.LookupEntityTypeCodeEnum;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.messaging.message.LocationFormDataCountAggregateEvent;
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
import org.checkerframework.checker.index.qual.SameLen;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataCountAggregateListener extends Listener {

  ReadOnlyKeyValueStore<String, LocationFormDataCountAggregateEvent> locationFormDataStringCount;
  private final MetadataService metadataService;
  private final EntityTagService entityTagService;
  private final PlanService planService;
  private final LocationService locationService;
  private final StreamsBuilderFactoryBean getKafkaStreams;
  private final KafkaProperties kafkaProperties;


  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('METADATA_COUNT_AGGREGATE')}", groupId = "reveal_server_group")
  public void listenGroupFoo(ConsumerRecord<String, LocationFormDataCountAggregateEvent> message) {
    init();
    locationFormDataStringCount = getKafkaStreams.getKafkaStreams().store(
        StoreQueryParameters.fromNameAndType(
            kafkaProperties.getStoreMap().get(KafkaConstants.locationFormDataStringCount),
            QueryableStoreTypes.keyValueStore()));
    log.info("Received Message k: {}  v: {}", message.key(), message.value());
    String k = message.key();
    LocationFormDataCountAggregateEvent aggMessage = message.value();

    String[] keySplit = k.split("_");

    String planId = keySplit[0];
    Plan plan = null;
    if (planId != null && !planId.equals("plan")) {
      plan = planService.findNullablePlanByIdentifier(UUID.fromString(planId));
    }

    String entityParentIdentifier = keySplit[2];

    Location location = locationService.findByIdentifier(
        UUID.fromString(entityParentIdentifier));

    UUID entityTagIdentifier = aggMessage.getEntityTagIdentifier();
    LocationFormDataCountAggregateEvent locationFormDataCountAggregateEvent = locationFormDataStringCount.get(
        k);

    if (locationFormDataCountAggregateEvent != null) {

      Optional<EntityTag> entityTagById = entityTagService.findEntityTagById(entityTagIdentifier);

      if (entityTagById.isPresent()) {
        EntityTag entityTag = entityTagById.get();

        if (locationFormDataCountAggregateEvent.getCount() != null) {

          String tag = entityTag.getTag() + "-count" + "_"
              + locationFormDataCountAggregateEvent.getCountKey();

          log.trace("tag: {}",tag);
          Optional<EntityTag> entityTagByTagName = entityTagService.getEntityTagByTagName(
              tag);

          EntityTag entityTag1;
          if (!entityTagByTagName.isPresent()) {
            EntityTagRequest entityTagRequest = EntityTagRequest.builder()
                .isAggregate(true)
                .addToMetadata(true)
                .tag(tag)
                .entityType(LookupEntityTypeCodeEnum.LOCATION_CODE)
                .scope(entityTag.getScope())
                .valueType(EntityTagDataTypes.INTEGER)
                .aggregationMethod(null)
                .formFieldNames(null)
                .definition("")
                .generated(true)
                .generationFormula(null)
                .isResultLiteral(false)
                .referencedFields(null)
                .resultExpression(null)
                .build();
            entityTag1 = entityTagService.createEntityTag(entityTagRequest,false);
          } else {
            entityTag1 = entityTagByTagName.get();
          }
          EntityTagEvent entityTagEvent1 = EntityTagEventFactory.getEntityTagEvent(entityTag1);
          if (entityTagEvent1.isAggregate()) {
            metadataService.updateMetaData(UUID.fromString(entityParentIdentifier),
                locationFormDataCountAggregateEvent.getCount().intValue(),
                plan, null,
                UserUtils.getCurrentPrincipleName(), entityTagEvent1.getValueType(),
                entityTagEvent1,
                "aggregate", location, "aggregate",
                Location.class,
                k + "-count" + "_" + locationFormDataCountAggregateEvent.getCountKey(), null);
          }

        }
      }
    }


  }
}
