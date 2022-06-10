package com.revealprecision.revealserver.messaging.streams;

import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.LocationAssigned;
import com.revealprecision.revealserver.messaging.message.LocationRelationshipMessage;
import com.revealprecision.revealserver.messaging.message.PlanLocationAssignMessage;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.PlanService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class LocationStream {

  private final LocationRelationshipService locationRelationshipService;
  private final LocationService locationService;
  private final KafkaProperties kafkaProperties;
  private final PlanService planService;
  private final Logger streamLog = LoggerFactory.getLogger("stream-file");

  @Bean
  KStream<String, LocationRelationshipMessage> getTotalStructures(
      StreamsBuilder streamsBuilder) {

    KStream<String, LocationRelationshipMessage> locationsImported = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(KafkaConstants.LOCATIONS_IMPORTED),
        Consumed.with(Serdes.String(), new JsonSerde<>(LocationRelationshipMessage.class)));

    KStream<String, LocationRelationshipMessage> structures = locationsImported
        .filter((k, locationRelationshipMessage) -> locationRelationshipMessage.getGeoName().equals(
            LocationConstants.STRUCTURE))
        .flatMapValues(
            (k, locationRelationshipMessage) -> locationRelationshipMessage.getAncestry().stream()
                .map(ancestor -> {
                  LocationRelationshipMessage locationRelationshipMessageWithAncestor = new LocationRelationshipMessage();
                  locationRelationshipMessageWithAncestor.setLocationName(
                      locationRelationshipMessage.getLocationName());
                  locationRelationshipMessageWithAncestor.setGeoName(locationRelationshipMessage.getGeoName());
                  locationRelationshipMessageWithAncestor.setAncestor(ancestor);
                  locationRelationshipMessageWithAncestor.setLocationIdentifier(
                      locationRelationshipMessage.getLocationIdentifier());
                  locationRelationshipMessageWithAncestor.setLocationHierarchyIdentifier(
                      locationRelationshipMessage.getLocationHierarchyIdentifier());
                  return locationRelationshipMessageWithAncestor;
                }).collect(Collectors.toList()))
        .selectKey((k, locationRelationshipMessage) ->
            locationRelationshipMessage.getLocationHierarchyIdentifier() + "_"
                + locationRelationshipMessage.getAncestor());

    structures.groupByKey().count(Materialized.as(kafkaProperties.getStoreMap()
        .get(KafkaConstants.structureCountPerParent)));

    return locationsImported;
  }

  @Bean
  KStream<String, PlanLocationAssignMessage> propogateLocationAssignment(
      StreamsBuilder streamsBuilder) {

    KStream<String, PlanLocationAssignMessage> locationsAssignedStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(KafkaConstants.PLAN_LOCATION_ASSIGNED),
        Consumed.with(Serdes.String(), new JsonSerde<>(PlanLocationAssignMessage.class)));

    //Doing this so that we can rewind this topic without rewinding to cause task generation from running again....
    locationsAssignedStream.to(kafkaProperties.getTopicMap().get(KafkaConstants.PLAN_LOCATION_ASSIGNED_STREAM));
    return locationsAssignedStream;
  }

  @Bean
  KStream<String, PlanLocationAssignMessage> getAssignedStructures(
      StreamsBuilder streamsBuilder) {

    // getting values from plan assignment
    KStream<String, PlanLocationAssignMessage> locationsAssignedStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(KafkaConstants.PLAN_LOCATION_ASSIGNED_STREAM),
        Consumed.with(Serdes.String(), new JsonSerde<>(PlanLocationAssignMessage.class)));

    locationsAssignedStream.peek((k,v)->streamLog.debug("locationsAssignedStream - k: {} v: {}", k,v));

    //TODO: remove hardcodings
    //Get structures from the locations assigned to plan
    KStream<String, PlanLocationAssignMessage> stringPlanLocationAssignMessageKStream = locationsAssignedStream
        .mapValues((k, planLocationAssignMessage) -> {
          planLocationAssignMessage.setLocationsRemoved(
              planLocationAssignMessage.getLocationsRemoved().stream()
                  .filter(locationRemoved -> locationService.findByIdentifier(
                          UUID.fromString(locationRemoved)).getGeographicLevel().getName()
                      .equals("operational")).collect(
                      Collectors.toList()));
          planLocationAssignMessage.setLocationsAdded(
              planLocationAssignMessage.getLocationsAdded().stream()
                  .filter(locationAdded -> locationService.findByIdentifier(
                          UUID.fromString(locationAdded)).getGeographicLevel().getName()
                      .equals("operational")).collect(
                      Collectors.toList()));
          return planLocationAssignMessage;
        });

    stringPlanLocationAssignMessageKStream.peek((k,v)->streamLog.debug("stringPlanLocationAssignMessageKStream - k: {} v: {}", k,v));

    KStream<String, LocationAssigned> stringLocationAssignedKStream = stringPlanLocationAssignMessageKStream
        .flatMapValues(
            (k, planLocationAssignMessage) -> getStructuresAssignedAndUnAssigned(
                planLocationAssignMessage))
        .flatMapValues((k, locationAssigned) -> getLocationAssignedsUnpackedByAncestry(locationAssigned))
        .selectKey((key, locationAssigned) -> getLocationPlanAncestorKey(locationAssigned))
        .mapValues((key, locationAssigned) -> locationAssigned.isAssigned() ? locationAssigned : null);

    stringLocationAssignedKStream.peek((k,v)->streamLog.debug("stringLocationAssignedKStream - k: {} v: {}", k,v));


    KTable<String, LocationAssigned> tableOfAssignedStructures = stringLocationAssignedKStream
        .repartition()
        .toTable(Materialized.<String, LocationAssigned, KeyValueStore<Bytes, byte[]>>as(kafkaProperties.getStoreMap()
            .get(KafkaConstants.tableOfAssignedStructuresWithParentKeyed))
            .withKeySerde(Serdes.String())
            .withValueSerde(new JsonSerde<>(LocationAssigned.class)));

    tableOfAssignedStructures.toStream().peek((k,v)->streamLog.debug("tableOfAssignedStructures.toStream() - k: {} v: {}", k,v));

    KTable<String, Long> count = tableOfAssignedStructures
        .groupBy((key, locationAssigned) -> KeyValue.pair(
                locationAssigned.getPlanIdentifier() + "_" + locationAssigned.getAncestor(),
                locationAssigned),
            Grouped.with(Serdes.String(), new JsonSerde<>(LocationAssigned.class)))
        .count(Materialized.as(
            kafkaProperties.getStoreMap().get(KafkaConstants.assignedStructureCountPerParent)));

    count.toStream().peek((k,v)->streamLog.debug("count.toStream() - k: {} v: {}", k,v));
    return locationsAssignedStream;
  }

  private String getLocationPlanAncestorKey(LocationAssigned locationAssigned) {
    return locationAssigned.getIdentifier() + "_"
        + locationAssigned.getPlanIdentifier() + "_" + locationAssigned.getAncestor();
  }

  private List<LocationAssigned> getLocationAssignedsUnpackedByAncestry(LocationAssigned locationAssigned) {
    return locationAssigned.getAncestry().stream().map(ancestor -> {
      LocationAssigned locationAssigned1 = new LocationAssigned();
      locationAssigned1.setAssigned(locationAssigned.isAssigned());
      locationAssigned1.setIdentifier(locationAssigned.getIdentifier());
      locationAssigned1.setPlanIdentifier(locationAssigned.getPlanIdentifier());
      locationAssigned1.setAncestor(ancestor);
      return locationAssigned1;
    }).collect(Collectors.toList());
  }


  private List<LocationAssigned> getStructuresAssignedAndUnAssigned(
      PlanLocationAssignMessage planLocationAssignMessage) {

    List<LocationAssigned> locations = new ArrayList<>();
    String planIdentifier = planLocationAssignMessage.getPlanIdentifier();

    Plan plan = planService.getPlanByIdentifier(UUID.fromString(planIdentifier));

    if (planLocationAssignMessage.getLocationsAdded() != null) {

      List<LocationAssigned> locationsAssigned = planLocationAssignMessage.getLocationsAdded()
          .stream().flatMap(locationAdded -> {
            List<Location> locations1 =
                locationRelationshipService.getChildrenLocations(
                    plan.getLocationHierarchy().getIdentifier(),
                    UUID.fromString(locationAdded));
            return locations1.stream();
          }).map(Location::getIdentifier)
          .map(locationIdentifier -> {
            LocationRelationship locationRelationshipsForLocation = locationRelationshipService.getLocationRelationshipsForLocation(
                plan.getLocationHierarchy().getIdentifier(), locationIdentifier);

            LocationAssigned locationAssigned = new LocationAssigned();
            locationAssigned.setAssigned(true);
            locationAssigned.setAncestry(locationRelationshipsForLocation.getAncestry());
            locationAssigned.setPlanIdentifier(planIdentifier);
            locationAssigned.setIdentifier(locationIdentifier.toString());

            return locationAssigned;
          })
          .collect(Collectors.toList());
      locations.addAll(locationsAssigned);
    }

    if (planLocationAssignMessage.getLocationsRemoved() != null) {
      List<LocationAssigned> locationsUnAssigned = planLocationAssignMessage.getLocationsRemoved()
          .stream().flatMap(locationRemoved -> {
            List<Location> locations1 =
                locationRelationshipService.getChildrenLocations(
                    plan.getLocationHierarchy().getIdentifier(),
                    UUID.fromString(locationRemoved));
            return locations1.stream();
          }).map(Location::getIdentifier)
          .map(locationIdentifier -> {
            LocationRelationship locationRelationshipsForLocation = locationRelationshipService.getLocationRelationshipsForLocation(
                plan.getLocationHierarchy().getIdentifier(), locationIdentifier);

            LocationAssigned locationAssigned = new LocationAssigned();
            locationAssigned.setAssigned(false);
            locationAssigned.setAncestry(locationRelationshipsForLocation.getAncestry());
            locationAssigned.setPlanIdentifier(planIdentifier);
            locationAssigned.setIdentifier(locationIdentifier.toString());

            return locationAssigned;
          })
          .collect(Collectors.toList());
      locations.addAll(locationsUnAssigned);
    }
    return locations;
  }
}
