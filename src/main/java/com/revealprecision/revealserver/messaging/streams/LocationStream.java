package com.revealprecision.revealserver.messaging.streams;

import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.HierarchyStructure;
import com.revealprecision.revealserver.messaging.message.LocationAfterJoin;
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
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Printed;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class LocationStream {

  private final LocationService locationService;

  private final LocationRelationshipService locationRelationshipService;

  private final PlanService planService;

  private final KafkaProperties kafkaProperties;

  @Bean
  KStream<String, PlanLocationAssignMessage> getAssignedStructures(
      StreamsBuilder streamsBuilder) {

    // getting values from plan assignment
    KStream<String, PlanLocationAssignMessage> locationsAssignedStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(KafkaConstants.PLAN_LOCATION_ASSIGNED),
        Consumed.with(Serdes.String(), new JsonSerde<>(PlanLocationAssignMessage.class)));
    locationsAssignedStream.print(Printed.<String, PlanLocationAssignMessage>toSysOut());

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

    KStream<String, LocationAssigned> stringLocationAssignedKStream = stringPlanLocationAssignMessageKStream
        .flatMapValues(
            (k, planLocationAssignMessage) -> getStructuresAssignedAndUnAssigned(
                planLocationAssignMessage))
        .flatMapValues((k, locationAssigned) -> {
          return locationAssigned.getAncestry().stream().map(ancestor -> {
            LocationAssigned locationAssigned1 = new LocationAssigned();
            locationAssigned1.setAssigned(locationAssigned.isAssigned());
            locationAssigned1.setIdentifier(locationAssigned.getIdentifier());
            locationAssigned1.setPlanIdentifier(locationAssigned.getPlanIdentifier());
            locationAssigned1.setAncestor(ancestor);
            return locationAssigned1;
          }).collect(Collectors.toList());
        })
        .peek((k, v) -> log.info("k: {},v: {}", k, v))
        .selectKey((key, locationAssigned) -> locationAssigned.getIdentifier() + "_"
            + locationAssigned.getPlanIdentifier() + "_" + locationAssigned.getAncestor())
        .mapValues((k, v) -> v.isAssigned() ? v : null);

    stringLocationAssignedKStream.to(kafkaProperties.getTopicMap().get(KafkaConstants.PLAN_STRUCTURES_ASSIGNED));

    KTable<String, LocationAssigned> tableOfAssignedStructures = streamsBuilder.table(
        kafkaProperties.getTopicMap().get(KafkaConstants.PLAN_STRUCTURES_ASSIGNED)
        , Consumed.with(Serdes.String(), new JsonSerde<>(LocationAssigned.class))
        , Materialized.as(kafkaProperties.getStoreMap().get(KafkaConstants.tableOfAssignedStructuresWithParentKeyed)));

    KTable<String, Long> assignedStructureCountPerParent = tableOfAssignedStructures
        .groupBy((key, locationAssigned) -> KeyValue.pair(
                locationAssigned.getPlanIdentifier()+"_"+locationAssigned.getAncestor(), locationAssigned),
            Grouped.with(Serdes.String(), new JsonSerde<>(LocationAssigned.class)))
        .count(Materialized.as(kafkaProperties.getStoreMap().get(KafkaConstants.assignedStructureCountPerParent)));
    assignedStructureCountPerParent.toStream()
        .print(Printed.<String, Long>toSysOut());

    return locationsAssignedStream;
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
