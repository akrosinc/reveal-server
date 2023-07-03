package com.revealprecision.revealserver.messaging.listener;

import static com.revealprecision.revealserver.api.v1.facade.factory.EntityMetaDataElasticFactory.getEntityMetadataElastic;

import com.revealprecision.revealserver.messaging.message.LocationIdEvent;
import com.revealprecision.revealserver.model.AggregateNumeric;
import com.revealprecision.revealserver.model.AggregateStringCount;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.es.EntityMetadataElastic;
import com.revealprecision.revealserver.persistence.es.HierarchyDetailsElastic;
import com.revealprecision.revealserver.persistence.es.LocationElastic;
import com.revealprecision.revealserver.persistence.projection.EventAggregateNumericProjection;
import com.revealprecision.revealserver.persistence.projection.EventAggregateStringCountProjection;
import com.revealprecision.revealserver.persistence.projection.ImportAggregateNumericProjection;
import com.revealprecision.revealserver.persistence.projection.ImportAggregateStringCountProjection;
import com.revealprecision.revealserver.persistence.repository.EventAggregateRepository;
import com.revealprecision.revealserver.persistence.repository.ImportAggregateRepository;
import com.revealprecision.revealserver.persistence.repository.LocationElasticRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRelationshipRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import com.revealprecision.revealserver.props.EventAggregationProperties;
import java.util.ArrayList;
import java.util.Arrays;
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
  private final LocationRelationshipRepository locationRelationshipRepository;
  private final EventAggregationProperties eventAggregationProperties;
  private final ImportAggregateRepository importAggregateRepository;


  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('EVENT_AGGREGATION_LOCATION')}", groupId = "reveal_server_group")
  public void processMessage(LocationIdEvent message) {
    log.debug("Received message {}", message);
    List<Location> locationsPage = locationRepository.findAllById(message.getUuids());
    List<LocationRelationship> locationRelationships = locationRelationshipRepository.getLocationRelationshipByLocation_IdentifierInAndLocationHierarchy_Identifier(
        new ArrayList<>(message.getUuids()), message.getHierarchyIdentifier());

    Map<UUID, LocationRelationship> locationRelationshipMap = locationRelationships.stream()
        .collect(
            Collectors.toMap(
                locationRelationship -> locationRelationship.getLocation().getIdentifier(),
                locationRelationship -> locationRelationship, (a, b) -> b));

    List<EventAggregateNumericProjection> aggregationValuesByLocations = eventAggregateRepository.getAggregationValuesByLocationList(
        message.getUuids().stream().map(UUID::toString).collect(Collectors.toList()));

    Map<String, List<AggregateNumeric>> eventAggregateListPerLocation = aggregationValuesByLocations.stream()
        .map(eventAggregateNumericProjection -> AggregateNumeric.builder()
            .avg(eventAggregateNumericProjection.getAvg())
            .locationIdentifier(eventAggregateNumericProjection.getLocationIdentifier())
            .fieldCode(eventAggregateNumericProjection.getFieldCode())
            .median(eventAggregateNumericProjection.getMedian())
            .sum(eventAggregateNumericProjection.getSum())
            .eventType(eventAggregateNumericProjection.getEventType())
            .planIdentifier(eventAggregateNumericProjection.getPlanIdentifier())
            .name(eventAggregateNumericProjection.getName())
            .build()
        )
        .collect(Collectors.groupingBy(
            AggregateNumeric::getLocationIdentifier));

    List<ImportAggregateNumericProjection> aggregationValuesByLocationList = importAggregateRepository.getAggregationValuesByLocationList(
        message.getUuids().stream().map(UUID::toString).collect(Collectors.toList()));

    Map<String, List<AggregateNumeric>> importAggregatePerLocation = aggregationValuesByLocationList.stream()
        .map(importAggregateNumericProjection -> AggregateNumeric.builder()
            .avg(importAggregateNumericProjection.getAvg())
            .locationIdentifier(importAggregateNumericProjection.getLocationIdentifier())
            .fieldCode(importAggregateNumericProjection.getFieldCode())
            .median(importAggregateNumericProjection.getMedian())
            .sum(importAggregateNumericProjection.getSum())
            .eventType(importAggregateNumericProjection.getEventType())
            .planIdentifier(importAggregateNumericProjection.getPlanIdentifier())
            .name(importAggregateNumericProjection.getName())
            .build()).collect(Collectors.groupingBy(
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

    List<EventAggregateStringCountProjection> eventAggregationCountsByLocations = eventAggregateRepository.getAggregationCountValuesByLocationList(
        message.getUuids().stream().map(UUID::toString).collect(Collectors.toList()));

    Map<String, List<AggregateStringCount>> eventAggregateStringCountListPerLocation = eventAggregationCountsByLocations.stream()
        .map(eventAggregateStringCountProjection -> AggregateStringCount.builder()
            .locationIdentifier(eventAggregateStringCountProjection.getLocationIdentifier())
            .fieldCode(eventAggregateStringCountProjection.getFieldCode())
            .fieldVal(eventAggregateStringCountProjection.getFieldVal())
            .eventType(eventAggregateStringCountProjection.getEventType())
            .planIdentifier(eventAggregateStringCountProjection.getPlanIdentifier())
            .name(eventAggregateStringCountProjection.getName())
            .count(eventAggregateStringCountProjection.getCount())
            .build()
        )
        .collect(Collectors.groupingBy(AggregateStringCount::getLocationIdentifier));

    List<ImportAggregateStringCountProjection> aggregationCountValuesByLocationList = importAggregateRepository.getAggregationCountValuesByLocationList(
        message.getUuids().stream().map(UUID::toString).collect(Collectors.toList()));

    Map<String, List<AggregateStringCount>> importAggregateStringCountListPerLocation = aggregationCountValuesByLocationList.stream()
        .map(eventAggregateStringCountProjection -> AggregateStringCount.builder()
            .locationIdentifier(eventAggregateStringCountProjection.getLocationIdentifier())
            .fieldCode(eventAggregateStringCountProjection.getFieldCode())
            .fieldVal(eventAggregateStringCountProjection.getFieldVal())
            .eventType(eventAggregateStringCountProjection.getEventType())
            .planIdentifier(eventAggregateStringCountProjection.getPlanIdentifier())
            .name(eventAggregateStringCountProjection.getName())
            .count(eventAggregateStringCountProjection.getCount())
            .build()
        )
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

      LocationRelationship relationship = locationRelationshipMap.get(
          location.getIdentifier());

      if (relationship != null) {
        Map<String, List<String>> stringListMap = new HashMap<>();
        if (relationship.getAncestry() != null) {
          stringListMap.put(message.getHierarchyIdentifier().toString(),
              relationship.getAncestry().stream().map(UUID::toString).collect(
                  Collectors.toList()));
          locationElastic.setAncestry(List.of(stringListMap));

          HierarchyDetailsElastic hierarchyDetailsElastic = new HierarchyDetailsElastic();
          hierarchyDetailsElastic.setAncestry(
              relationship.getAncestry().stream().map(UUID::toString).collect(
                  Collectors.toList()));

          hierarchyDetailsElastic.setParent(
              relationship.getParentLocation().getIdentifier().toString());
          List<String> strings = Arrays.asList(message.getNodeOrder().split(","));
          hierarchyDetailsElastic.setGeographicLevelNumber(
              strings.indexOf(location.getGeographicLevel().getName()));

          Map<String, HierarchyDetailsElastic> hierarchyDetailsElasticMap = new HashMap<>();
          hierarchyDetailsElasticMap.put(message.getHierarchyIdentifier().toString(),
              hierarchyDetailsElastic);

          locationElastic.setHierarchyDetailsElastic(hierarchyDetailsElasticMap);
          log.trace("SAVED----------------------{}", location.getName());
        } else {
          log.trace("NOT SAVED----------------------");
        }
      }

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
                if (eventAggregateNumericProjection.getEventType().equals("Import")) {
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
                    s + "sum",
                    eventAggregateNumericProjection.getSum());
                entityMetadataElastics.add(entityMetadataElasticSum);

                EntityMetadataElastic entityMetadataElasticsAverage = getEntityMetadataElastic(
                    eventAggregateNumericProjection.getPlanIdentifier(),
                    s + "average",
                    eventAggregateNumericProjection.getAvg());
                entityMetadataElastics.add(entityMetadataElasticsAverage);

                EntityMetadataElastic entityMetadataElasticsMedian = getEntityMetadataElastic(
                    eventAggregateNumericProjection.getPlanIdentifier(),
                    s + "median",
                    eventAggregateNumericProjection.getAvg());
                entityMetadataElastics.add(entityMetadataElasticsMedian);

                return entityMetadataElastics.stream();
              }).collect(Collectors.toList());

          log.trace("-----------------------------");

          List<EntityMetadataElastic> metadata = locationElastic.getMetadata();

          List<EntityMetadataElastic> existingTags = metadata.stream()
              .filter(
                  metadataItem -> !anImport.stream().map(EntityMetadataElastic::getTag).collect(
                      Collectors.toList()).contains(metadataItem.getTag()))
              .collect(Collectors.toList());

          existingTags.addAll(anImport);

          locationElastic.setMetadata(existingTags);

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
                if (aggregationStringCountProjection.getEventType().equals("Import")) {
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
                    aggregationStringCountProjection.getCount());
                return entityMetadataElastic;
              }).collect(Collectors.toList());

          log.trace("-----------------------------");

          List<EntityMetadataElastic> metadata = locationElastic.getMetadata();

          List<EntityMetadataElastic> existingTags = metadata.stream()
              .filter(
                  metadataItem -> !anImport.stream().map(EntityMetadataElastic::getTag).collect(
                      Collectors.toList()).contains(metadataItem.getTag()))
              .collect(Collectors.toList());

          existingTags.addAll(anImport);

          locationElastic.setMetadata(existingTags);

        } else {
          log.trace("NO METADATA----------------------");
        }
      }

      elastics.add(locationElastic);

    });

    if (elastics.size() > 0) {

      locationElasticRepository.saveAll(elastics);
      log.info("saved");
    } else {
      log.info("not saved");
    }
  }


}
