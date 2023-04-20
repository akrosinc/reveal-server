package com.revealprecision.revealserver.messaging.listener;

import static com.revealprecision.revealserver.api.v1.facade.factory.EntityMetaDataElasticFactory.getEntityMetadataElastic;

import com.revealprecision.revealserver.messaging.message.LocationIdEvent;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.es.EntityMetadataElastic;
import com.revealprecision.revealserver.persistence.es.HierarchyDetailsElastic;
import com.revealprecision.revealserver.persistence.es.LocationElastic;
import com.revealprecision.revealserver.persistence.projection.EventAggregationNumericProjection;
import com.revealprecision.revealserver.persistence.projection.EventAggregationStringCountProjection;
import com.revealprecision.revealserver.persistence.repository.EventAggregationRepository;
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
@Profile("Simulation & (Listening | location-id-listener))")
public class LocationIdListener extends Listener {

  private final LocationElasticRepository locationElasticRepository;
  private final EventAggregationRepository eventAggregationRepository;
  private final LocationRepository locationRepository;
  private final LocationRelationshipRepository locationRelationshipRepository;
  private final EventAggregationProperties eventAggregationProperties;


  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('EVENT_AGGREGATION_LOCATION')}", groupId = "reveal_server_group" )
  public void processMessage(LocationIdEvent message) {
    log.debug("Received message {}",message);
    List<Location> locationsPage = locationRepository.findAllById(message.getUuids());
    locationsPage.forEach(location -> log.info(location.getName()));
    List<LocationRelationship> locationRelationships = locationRelationshipRepository.getLocationRelationshipByLocation_IdentifierInAndLocationHierarchy_Identifier(
        new ArrayList<>(message.getUuids()), message.getHierarchyIdentifier());

    Map<UUID, LocationRelationship> locationRelationshipMap = locationRelationships.stream()
        .collect(
            Collectors.toMap(
                locationRelationship -> locationRelationship.getLocation().getIdentifier(),
                locationRelationship -> locationRelationship, (a, b) -> b));

    List<EventAggregationNumericProjection> aggregationValuesByLocations = eventAggregationRepository.getAggregationValuesByLocationList(
        message.getUuids().stream().map(UUID::toString).collect(Collectors.toList()));

    Map<String, List<EventAggregationNumericProjection>> aggregateListPerLocation = aggregationValuesByLocations.stream()
        .collect(Collectors.groupingBy(
            EventAggregationNumericProjection::getLocationIdentifier));

    List<EventAggregationStringCountProjection> aggregationCountsByLocations = eventAggregationRepository.getAggregationCountValuesByLocationList(
        message.getUuids().stream().map(UUID::toString).collect(Collectors.toList()));

    Map<String, List<EventAggregationStringCountProjection>> aggregateCountMap = aggregationCountsByLocations.stream()
        .collect(Collectors.groupingBy(
            EventAggregationStringCountProjection::getLocationIdentifier));

    List<LocationElastic> allById = locationElasticRepository.findAllByIdIn(
        message.getUuids().stream().map(UUID::toString).collect(
            Collectors.toList()));

    List<LocationElastic> elastics = new ArrayList<>();

    Map<String, LocationElastic> collect = allById.stream().collect(
        Collectors.toMap(LocationElastic::getId, locationElastic -> locationElastic, (a, b) -> b));

    locationsPage.stream().forEach(location -> {

      LocationElastic locationElastic =
          collect.containsKey(location.getIdentifier().toString())?collect.get(location.getIdentifier().toString()):new LocationElastic();

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
          hierarchyDetailsElasticMap.put(message.getHierarchyIdentifier().toString(), hierarchyDetailsElastic);

          locationElastic.setHierarchyDetailsElastic(hierarchyDetailsElasticMap);
          log.trace("SAVED----------------------{}", location.getName());
        } else {
          log.trace("NOT SAVED----------------------");
        }
      }


      if (aggregateListPerLocation.containsKey(location.getIdentifier().toString())) {
        List<EventAggregationNumericProjection> eventAggregationNumericProjections = aggregateListPerLocation.get(
            location.getIdentifier().toString());

        if (eventAggregationNumericProjections.size() > 0) {
          List<EntityMetadataElastic> anImport = eventAggregationNumericProjections.stream()
              .filter(
                  aggregationStringCountProjection -> !aggregationStringCountProjection.getFieldCode()
                      .matches(eventAggregationProperties.getExclusionListRegex()))
              .flatMap(eventAggregationNumericProjection -> {
                List<EntityMetadataElastic> entityMetadataElastics = new ArrayList<>();

                EntityMetadataElastic entityMetadataElasticSum = getEntityMetadataElastic(
                    eventAggregationNumericProjection.getPlanIdentifier(),
                    eventAggregationNumericProjection.getEventType() + "_" +
                        eventAggregationNumericProjection.getFieldCode() + "_sum",
                    eventAggregationNumericProjection.getSum());
                entityMetadataElastics.add(entityMetadataElasticSum);

                EntityMetadataElastic entityMetadataElasticsAverage = getEntityMetadataElastic(
                    eventAggregationNumericProjection.getPlanIdentifier(),
                    eventAggregationNumericProjection.getEventType() + "_" +
                        eventAggregationNumericProjection.getFieldCode() + "_average",
                    eventAggregationNumericProjection.getAvg());
                entityMetadataElastics.add(entityMetadataElasticsAverage);

                EntityMetadataElastic entityMetadataElasticsMedian = getEntityMetadataElastic(
                    eventAggregationNumericProjection.getPlanIdentifier(),
                    eventAggregationNumericProjection.getEventType() + "_" +
                        eventAggregationNumericProjection.getFieldCode() + "_median",
                    eventAggregationNumericProjection.getAvg());
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


      if (aggregateCountMap
          .containsKey(location.getIdentifier().toString())) {
        List<EventAggregationStringCountProjection> aggregationStringCountProjections = aggregateCountMap.get(
            location.getIdentifier().toString());

        if (aggregationStringCountProjections.size() > 0) {
          List<EntityMetadataElastic> anImport = aggregationStringCountProjections.stream()
              .filter(
                  aggregationStringCountProjection -> !aggregationStringCountProjection.getFieldCode()
                      .matches(eventAggregationProperties.getExclusionListRegex()))
              .map(aggregationStringCountProjection -> {
                EntityMetadataElastic entityMetadataElastic = getEntityMetadataElastic(
                    aggregationStringCountProjection.getPlanIdentifier(),
                    aggregationStringCountProjection.getEventType() + "_"
                        + aggregationStringCountProjection.getFieldCode() + "_"
                        + aggregationStringCountProjection.getFieldVal() + "_count",
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

      if (locationElastic.getHierarchyDetailsElastic() != null) {
        elastics.add(locationElastic);
      }

    });

    if (elastics.size() > 0) {
      locationElasticRepository.saveAll(elastics);
    }
  }


}
