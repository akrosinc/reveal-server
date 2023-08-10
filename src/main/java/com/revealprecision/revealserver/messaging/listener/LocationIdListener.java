package com.revealprecision.revealserver.messaging.listener;

import static com.revealprecision.revealserver.api.v1.facade.factory.EntityMetaDataElasticFactory.getEntityMetadataElastic;

import com.revealprecision.revealserver.constants.EntityTagDataAggregationMethods;
import com.revealprecision.revealserver.constants.EntityTagFieldTypes;
import com.revealprecision.revealserver.messaging.message.LocationIdEvent;
import com.revealprecision.revealserver.model.AggregateNumeric;
import com.revealprecision.revealserver.model.AggregateStringCount;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.es.EntityMetadataElastic;
import com.revealprecision.revealserver.persistence.es.LocationElastic;
import com.revealprecision.revealserver.persistence.projection.AggregateNumericProjection;
import com.revealprecision.revealserver.persistence.projection.AggregateStringProjection;
import com.revealprecision.revealserver.persistence.projection.EventAggregateNumericProjection;
import com.revealprecision.revealserver.persistence.projection.EventAggregateStringCountProjection;
import com.revealprecision.revealserver.persistence.projection.ImportAggregateNumericProjection;
import com.revealprecision.revealserver.persistence.projection.ImportAggregateStringCountProjection;
import com.revealprecision.revealserver.persistence.projection.ResourceAggregateNumericProjection;
import com.revealprecision.revealserver.persistence.repository.EventAggregateRepository;
import com.revealprecision.revealserver.persistence.repository.ImportAggregateRepository;
import com.revealprecision.revealserver.persistence.repository.LocationElasticRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import com.revealprecision.revealserver.persistence.repository.ResourceAggregateRepository;
import com.revealprecision.revealserver.props.EventAggregationProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class LocationIdListener extends Listener {

  private final LocationElasticRepository locationElasticRepository;
  private final EventAggregateRepository eventAggregateRepository;
  private final LocationRepository locationRepository;
  private final EventAggregationProperties eventAggregationProperties;
  private final ImportAggregateRepository importAggregateRepository;
  private final ResourceAggregateRepository resourceAggregateRepository;


  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('EVENT_AGGREGATION_LOCATION')}", groupId = "reveal_server_group")
  public void processMessage(LocationIdEvent message) {
    log.debug("Received message {}", message);
    List<Location> locationsPage = locationRepository.findAllById(message.getUuids());

    List<EventAggregateNumericProjection> eventAggregationValuesByLocations = eventAggregateRepository.getAggregationValuesByLocationList(
        message.getUuids().stream().map(UUID::toString).collect(Collectors.toList()));

    Map<String, List<AggregateNumeric>> eventAggregateListPerLocation = eventAggregationValuesByLocations.stream()
        .map(aggregateNumeric -> getBuild(aggregateNumeric,
            EntityTagFieldTypes.EVENT.concat("-")
                .concat(aggregateNumeric.getEventType())))
        .collect(Collectors.groupingBy(
            AggregateNumeric::getLocationIdentifier));

    List<ImportAggregateNumericProjection> importAggregationValuesByLocationList = importAggregateRepository.getAggregationValuesByLocationList(
        message.getUuids().stream().map(UUID::toString).collect(Collectors.toList()),
        message.getHierarchyIdentifier());

    Map<String, List<AggregateNumeric>> importAggregatePerLocation = importAggregationValuesByLocationList.stream()
        .map(aggregateNumeric -> getBuild(aggregateNumeric, EntityTagFieldTypes.IMPORT))
        .collect(Collectors.groupingBy(
            AggregateNumeric::getLocationIdentifier));

    List<ResourceAggregateNumericProjection> resourceAggregationValuesByLocation = resourceAggregateRepository.getAggregationValuesByLocationList(
        message.getUuids().stream().map(UUID::toString).collect(Collectors.toList()),
        message.getHierarchyIdentifier());

    Map<String, List<AggregateNumeric>> resourceAggregatePerLocation = resourceAggregationValuesByLocation.stream()
        .map(aggregateNumeric -> getBuild(aggregateNumeric,
            EntityTagFieldTypes.RESOURCE_PLANNING.concat("-").concat(aggregateNumeric.getEventType())))
        .collect(Collectors.groupingBy(
            AggregateNumeric::getLocationIdentifier));

    Map<String, List<AggregateNumeric>> numericAggregatePerLocation = new HashMap<>(
        eventAggregateListPerLocation);

    importAggregatePerLocation.forEach((key, value) -> {
      numericAggregatePerLocation.merge(key, value, (v1, v2) -> {
        List<AggregateNumeric> aggregateNumerics = new ArrayList<>();
        aggregateNumerics.addAll(v1);
        aggregateNumerics.addAll(v2);
        return aggregateNumerics;
      });
    });

    resourceAggregatePerLocation.forEach((key, value) -> {
      numericAggregatePerLocation.merge(key, value, (v1, v2) -> {
        List<AggregateNumeric> aggregateNumerics = new ArrayList<>();
        aggregateNumerics.addAll(v1);
        aggregateNumerics.addAll(v2);
        return aggregateNumerics;
      });
    });

    List<EventAggregateStringCountProjection> eventAggregationCountsByLocations = eventAggregateRepository.getAggregationCountValuesByLocationList(
        message.getUuids().stream().map(UUID::toString).collect(Collectors.toList()));

    Map<String, List<AggregateStringCount>> eventAggregateStringCountListPerLocation = eventAggregationCountsByLocations.stream()
        .map(aggregateNumeric -> getBuild(aggregateNumeric,
            EntityTagFieldTypes.EVENT.concat("-").concat(aggregateNumeric.getEventType())))
        .collect(Collectors.groupingBy(AggregateStringCount::getLocationIdentifier));

    List<ImportAggregateStringCountProjection> aggregationCountValuesByLocationList = importAggregateRepository.getAggregationCountValuesByLocationList(
        message.getUuids().stream().map(UUID::toString).collect(Collectors.toList()),
        message.getHierarchyIdentifier());

    Map<String, List<AggregateStringCount>> importAggregateStringCountListPerLocation = aggregationCountValuesByLocationList.stream()
        .map(aggregateNumeric -> getBuild(aggregateNumeric, EntityTagFieldTypes.IMPORT))
        .collect(Collectors.groupingBy(AggregateStringCount::getLocationIdentifier));

    Map<String, List<AggregateStringCount>> stringAggregatePerLocation = new HashMap<>(
        eventAggregateStringCountListPerLocation);

    importAggregateStringCountListPerLocation.forEach((key, value) -> {
      stringAggregatePerLocation.merge(key, value, (v1, v2) -> {
        List<AggregateStringCount> aggregateNumerics = new ArrayList<>();
        aggregateNumerics.addAll(v1);
        aggregateNumerics.addAll(v2);
        return aggregateNumerics;
      });
    });

    List<LocationElastic> allById = locationElasticRepository.findAllByIdIn(
        message.getUuids().stream().map(UUID::toString).collect(
            Collectors.toList()));

    List<LocationElastic> elastics = new ArrayList<>();

    Map<String, LocationElastic> collect = allById.stream().collect(
        Collectors.toMap(LocationElastic::getId, locationElastic -> locationElastic, (a, b) -> b));

    locationsPage.stream().forEach(location -> {

      LocationElastic locationElastic =
          collect.containsKey(location.getIdentifier().toString()) ? collect.get(
              location.getIdentifier().toString()) : new LocationElastic();

      locationElastic.setId(location.getIdentifier().toString());
      locationElastic.setExternalId(location.getExternalId().toString());
      locationElastic.setName(location.getName());
      locationElastic.setGeometry(location.getGeometry());
      locationElastic.setLevel(location.getGeographicLevel().getName());

      if (numericAggregatePerLocation.containsKey(location.getIdentifier().toString())) {
        List<AggregateNumeric> eventAggregateNumericProjections = numericAggregatePerLocation.get(
            location.getIdentifier().toString());

        if (eventAggregateNumericProjections.size() > 0) {
          List<EntityMetadataElastic> anImport = eventAggregateNumericProjections.stream()
              .filter(
                  aggregationStringCountProjection -> !aggregationStringCountProjection.getFieldCode()
                      .matches(eventAggregationProperties.getExclusionListRegex()))
              .flatMap(eventAggregateNumericProjection -> {
                List<EntityMetadataElastic> entityMetadataElastics = new ArrayList<>();

                String s;
                if (eventAggregateNumericProjection.getEventType()
                    .equals(EntityTagFieldTypes.IMPORT)) {
                  s = eventAggregateNumericProjection.getFieldCode()
                      + eventAggregationProperties.getDelim();
                } else {
                  s = eventAggregateNumericProjection.getEventType()
                      + eventAggregationProperties.getDelim() +
                      eventAggregateNumericProjection.getFieldCode()
                      + eventAggregationProperties.getDelim();
                }

                EntityMetadataElastic entityMetadataElasticSum = getEntityMetadataElastic(
                    eventAggregateNumericProjection.getPlanIdentifier(),
                    s + EntityTagDataAggregationMethods.SUM,
                    eventAggregateNumericProjection.getSum(), message.getHierarchyIdentifier(),
                    eventAggregateNumericProjection.getFieldType());
                entityMetadataElastics.add(entityMetadataElasticSum);

                EntityMetadataElastic entityMetadataElasticsAverage = getEntityMetadataElastic(
                    eventAggregateNumericProjection.getPlanIdentifier(),
                    s + EntityTagDataAggregationMethods.AVERAGE,
                    eventAggregateNumericProjection.getAvg(), message.getHierarchyIdentifier(),
                    eventAggregateNumericProjection.getFieldType());
                entityMetadataElastics.add(entityMetadataElasticsAverage);

                EntityMetadataElastic entityMetadataElasticsMedian = getEntityMetadataElastic(
                    eventAggregateNumericProjection.getPlanIdentifier(),
                    s + EntityTagDataAggregationMethods.MEDIAN,
                    eventAggregateNumericProjection.getMedian(), message.getHierarchyIdentifier(),
                    eventAggregateNumericProjection.getFieldType());
                entityMetadataElastics.add(entityMetadataElasticsMedian);

                EntityMetadataElastic entityMetadataElasticsMax = getEntityMetadataElastic(
                    eventAggregateNumericProjection.getPlanIdentifier(),
                    s + EntityTagDataAggregationMethods.MAX,
                    eventAggregateNumericProjection.getMax(), message.getHierarchyIdentifier(),
                    eventAggregateNumericProjection.getFieldType());
                entityMetadataElastics.add(entityMetadataElasticsMax);

                EntityMetadataElastic entityMetadataElasticsMin = getEntityMetadataElastic(
                    eventAggregateNumericProjection.getPlanIdentifier(),
                    s + EntityTagDataAggregationMethods.MIN,
                    eventAggregateNumericProjection.getMin(), message.getHierarchyIdentifier(),
                    eventAggregateNumericProjection.getFieldType());
                entityMetadataElastics.add(entityMetadataElasticsMin);

                return entityMetadataElastics.stream();
              }).collect(Collectors.toList());

          log.trace("-----------------------------");

          List<EntityMetadataElastic> metadata = locationElastic.getMetadata();

          if (metadata != null) {
            List<EntityMetadataElastic> existingTags = metadata.stream()
                .filter(
                    metadataItem -> !anImport.stream().map(EntityMetadataElastic::getTag).collect(
                        Collectors.toList()).contains(metadataItem.getTag()))
                .collect(Collectors.toList());

            existingTags.addAll(anImport);
            locationElastic.setMetadata(existingTags);
          }
        } else {
          log.trace("NO METADATA----------------------");
        }

      }

      if (stringAggregatePerLocation
          .containsKey(location.getIdentifier().toString())) {
        List<AggregateStringCount> aggregationStringCountProjections = stringAggregatePerLocation.get(
            location.getIdentifier().toString());

        if (aggregationStringCountProjections.size() > 0) {
          List<EntityMetadataElastic> anImport = aggregationStringCountProjections.stream()
              .filter(
                  aggregationStringCountProjection -> !aggregationStringCountProjection.getFieldCode()
                      .matches(eventAggregationProperties.getExclusionListRegex()))
              .map(aggregationStringCountProjection -> {
                String s;
                if (aggregationStringCountProjection.getEventType()
                    .equals(EntityTagFieldTypes.IMPORT)) {
                  s = aggregationStringCountProjection.getFieldCode()
                      + eventAggregationProperties.getDelim()
                      + aggregationStringCountProjection.getFieldVal()
                      + eventAggregationProperties.getDelim();
                } else {
                  s = aggregationStringCountProjection.getEventType()
                      + eventAggregationProperties.getDelim()
                      + aggregationStringCountProjection.getFieldCode()
                      + eventAggregationProperties.getDelim()
                      + aggregationStringCountProjection.getFieldVal()
                      + eventAggregationProperties.getDelim();
                }

                String eventAggregationNumericProjection1 =
                    s + "count";
                EntityMetadataElastic entityMetadataElastic = getEntityMetadataElastic(
                    aggregationStringCountProjection.getPlanIdentifier(),
                    eventAggregationNumericProjection1,
                    aggregationStringCountProjection.getCount(), message.getHierarchyIdentifier(),
                    aggregationStringCountProjection.getFieldType());
                return entityMetadataElastic;
              }).collect(Collectors.toList());

          log.trace("-----------------------------");

          List<EntityMetadataElastic> metadata = locationElastic.getMetadata();

          if (metadata != null) {
            List<EntityMetadataElastic> existingTags = metadata.stream()
                .filter(
                    metadataItem -> !anImport.stream().map(EntityMetadataElastic::getTag).collect(
                        Collectors.toList()).contains(metadataItem.getTag()))
                .collect(Collectors.toList());

            existingTags.addAll(anImport);
            locationElastic.setMetadata(existingTags);
          }
        } else {
          log.trace("NO METADATA----------------------");
        }
      }

      elastics.add(locationElastic);

    });

    if (elastics.size() > 0) {

      locationElasticRepository.saveAll(elastics);
      log.debug("saved");
    } else {
      log.debug("not saved");
    }
  }

  private <T extends AggregateStringProjection> AggregateStringCount getBuild(
      T eventAggregateStringCountProjection, String fieldType) {
    return AggregateStringCount.builder()
        .locationIdentifier(eventAggregateStringCountProjection.getLocationIdentifier())
        .fieldCode(eventAggregateStringCountProjection.getFieldCode())
        .fieldVal(eventAggregateStringCountProjection.getFieldVal())
        .fieldType(fieldType)
        .eventType(eventAggregateStringCountProjection.getEventType())
        .planIdentifier(eventAggregateStringCountProjection.getPlanIdentifier())
        .name(eventAggregateStringCountProjection.getName())
        .count(eventAggregateStringCountProjection.getCount())
        .build();
  }

  private <T extends AggregateNumericProjection> AggregateNumeric getBuild(
      T aggregateNumericProjection, String fieldType) {

    return AggregateNumeric.builder()
        .avg(aggregateNumericProjection.getAvg())
        .locationIdentifier(aggregateNumericProjection.getLocationIdentifier())
        .fieldCode(aggregateNumericProjection.getFieldCode())
        .median(aggregateNumericProjection.getMedian())
        .sum(aggregateNumericProjection.getSum())
        .fieldType(fieldType)
        .eventType(aggregateNumericProjection.getEventType())
        .planIdentifier(aggregateNumericProjection.getPlanIdentifier())
        .name(aggregateNumericProjection.getName())
        .min(aggregateNumericProjection.getMin())
        .max(aggregateNumericProjection.getMax())
        .build();
  }


}
