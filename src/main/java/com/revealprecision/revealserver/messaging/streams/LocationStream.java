package com.revealprecision.revealserver.messaging.streams;

import static com.revealprecision.revealserver.messaging.TopicConstants.JOINED_STRUCTURE_TABLE;
import static com.revealprecision.revealserver.messaging.TopicConstants.STRUCTURES_IMPORTED;
import static com.revealprecision.revealserver.messaging.TopicConstants.STRUCTURES_IMPORTED_FOR_TASK;

import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.messaging.TopicConstants;
import com.revealprecision.revealserver.messaging.message.HierarchyStructure;
import com.revealprecision.revealserver.messaging.message.LocationAfterJoin;
import com.revealprecision.revealserver.messaging.message.LocationAncestor;
import com.revealprecision.revealserver.messaging.message.LocationAssigned;
import com.revealprecision.revealserver.messaging.message.LocationRelationshipMessage;
import com.revealprecision.revealserver.messaging.message.PlanLocationAssignMessage;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.projection.LocationProjection;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.PlanService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
  KStream<String, LocationRelationshipMessage> getLocationsRelationships2(
      StreamsBuilder streamsBuilder) {

    //locations imported
    KStream<String, LocationRelationshipMessage> locationImportStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(TopicConstants.LOCATIONS_IMPORTED),
        Consumed.with(Serdes.String(), new JsonSerde<>(LocationRelationshipMessage.class)));
    locationImportStream.print(Printed.<String, LocationRelationshipMessage>toSysOut());

    // locations imported filtered by structures
    KStream<String, LocationRelationshipMessage> locationImportStreamKeyedAndFiltered = locationImportStream
        .selectKey((k, v) -> v.getLocationIdentifier().toString())
        .filter(((key, location) -> location.getGeoName()
            .equals(LocationConstants.STRUCTURE)))
        .mapValues((k, location) -> {

          List<LocationAncestor> locationAncestors = IntStream.range(0,
                  location.getAncestry().size() )
              .mapToObj(index -> {
                Location ancestorLocation = locationService.findByIdentifier(
                    location.getAncestry().get(index));
                LocationAncestor locationAncestor = new LocationAncestor();
                locationAncestor.setLocationId(ancestorLocation.getIdentifier().toString());
                locationAncestor.setGeoName(ancestorLocation.getGeographicLevel().getName());
                locationAncestor.setNodePosition(index);

                return locationAncestor;
              }).collect(Collectors.toList());
          location.setLocationAncestors(locationAncestors);
          return location;
        });
    locationImportStreamKeyedAndFiltered.to(kafkaProperties.getTopicMap().get(STRUCTURES_IMPORTED));
    locationImportStreamKeyedAndFiltered.to(kafkaProperties.getTopicMap().get(STRUCTURES_IMPORTED_FOR_TASK));
    locationImportStreamKeyedAndFiltered.print(
        Printed.<String, LocationRelationshipMessage>toSysOut());

    //storing structures imported
    KTable<String, LocationRelationshipMessage> structuresImportedTable = streamsBuilder.table(
        kafkaProperties.getTopicMap().get(STRUCTURES_IMPORTED),
        Consumed.with(Serdes.String(), new JsonSerde<>(LocationRelationshipMessage.class)));

    KStream<String, HierarchyStructure> hierarchyStructuresStream = getStringHierarchyStructureKStream(
        locationImportStreamKeyedAndFiltered);
    hierarchyStructuresStream.to(kafkaProperties.getTopicMap().get(TopicConstants.HIERARCHY_STRUCTURES_IMPORTED));
    hierarchyStructuresStream.print(Printed.<String, HierarchyStructure>toSysOut());

    // Create Table and group by parent, count per parent
    KTable<String, Long> structureCountPerParent = hierarchyStructuresStream
        .groupBy((key, hierarchyStructure) -> key.split("_")[1],
            Grouped.with(Serdes.String(), new JsonSerde<>(HierarchyStructure.class)))
        .count(Materialized.as(kafkaProperties.getStoreMap().get(TopicConstants.structureCountPerParent)));
    structureCountPerParent.toStream().print(Printed.<String, Long>toSysOut());

    // getting values from plan assignment
    KStream<String, PlanLocationAssignMessage> locationsAssignedStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(TopicConstants.PLAN_LOCATION_ASSIGNED),
        Consumed.with(Serdes.String(), new JsonSerde<>(PlanLocationAssignMessage.class)));

    //Get structures from the locations assigned to plan
    KStream<String, LocationAssigned> structuresAssignedStreamKeyedAndFiltered = locationsAssignedStream.flatMapValues(
            //structures added and removed from plan
            (k, planLocationAssignMessage) -> getStructuresAssignedAndUnAssigned(
                planLocationAssignMessage))
        .selectKey((key, locationAssigned) -> locationAssigned.getIdentifier());
    structuresAssignedStreamKeyedAndFiltered.to(kafkaProperties.getTopicMap().get(TopicConstants.PLAN_STRUCTURES_ASSIGNED));
    structuresAssignedStreamKeyedAndFiltered.print(Printed.<String, LocationAssigned>toSysOut());

    //This table will have assigned and unassigned structures joined to get their corresponding structures from the import
    KStream<String, LocationAfterJoin> assignedStructuresWithParent = structuresAssignedStreamKeyedAndFiltered
        .join(structuresImportedTable
            , (locationAssigned, locationRelationship) -> {
              LocationAfterJoin locationAfterJoin = new LocationAfterJoin();
              locationAfterJoin.setLocationIdentifier(
                  locationRelationship.getLocationIdentifier());
              locationAfterJoin.setParentLocationIdentifier(
                  locationRelationship.getParentLocationIdentifier());
              locationAfterJoin.setGeoName(locationRelationship.getGeoName());
              locationAfterJoin.setAssigned(locationAssigned.isAssigned());
              locationAfterJoin.setAncestry(locationRelationship.getLocationAncestors());
              locationAfterJoin.setPlanIdentifier(locationAssigned.getPlanIdentifier());
              return locationAfterJoin;
            });

    //Setting the key for this stream which is the structure id
    KStream<String, LocationAfterJoin> assignedStructuresWithParentKeyed = assignedStructuresWithParent
        .selectKey((k, v) -> v.getLocationIdentifier().toString());
//        .mapValues((k, v) -> v.isAssigned() ? v : null);

    assignedStructuresWithParentKeyed.to(kafkaProperties.getTopicMap().get(JOINED_STRUCTURE_TABLE));
    assignedStructuresWithParentKeyed.print(Printed.<String, LocationAfterJoin>toSysOut());

    //this flattens the structures such that each structure is repeated for each parent in the hierarchy going upwards
    // i.e
    // incoming: -> structure1Id = structure1, [oa1, catchment1, province1, country1]
    // outgoing: -> [structure1_oa1Id        = structure1, oa1
    //               structure1_catchment1Id = structure1, catchment1
    //               structure1_province1Id  = structure1, province1
    //               structure1_country1Id   = structure1, country1]
    //
    //         the key is set as illustrated above
    //             if the structure is not assigned to the plan then the payload is set to null
    KStream<String, HierarchyStructure> hierarchyStructuresAssignedStream = getHierarchyStructureKStream(
        assignedStructuresWithParentKeyed);

    hierarchyStructuresAssignedStream.to(
        kafkaProperties.getTopicMap().get(TopicConstants.HIERARCHY_STRUCTURES_ASSIGNED));
    hierarchyStructuresAssignedStream.print(Printed.<String, HierarchyStructure>toSysOut());

    //Expecting to remove the structure duplicates here i.e. remove the unassigned from the table and insert assigned
    //    duplicate keys in table will update each other.
    //    the nulls (as a result of not being assigned) from above will delete the previous records
    //    -> i.e. we will therefore only count what is remaining which are the assigned structures
    KTable<String, HierarchyStructure> tableOfAssignedStructures = streamsBuilder.table(
        kafkaProperties.getTopicMap().get(TopicConstants.HIERARCHY_STRUCTURES_ASSIGNED)
        , Consumed.with(Serdes.String(), new JsonSerde<>(HierarchyStructure.class))
        , Materialized.as(kafkaProperties.getStoreMap().get(TopicConstants.tableOfAssignedStructuresWithParentKeyed)));


    // Create Table and group by parent, count per parent
    KTable<String, Long> assignedStructureCountPerParent = tableOfAssignedStructures
        .groupBy((key, hierarchyStructure) -> KeyValue.pair(
                 key.split("_")[1].concat("_").concat(key.split("_")[2]), hierarchyStructure),
            Grouped.with(Serdes.String(), new JsonSerde<>(HierarchyStructure.class)))
        .count(Materialized.as(kafkaProperties.getStoreMap().get(TopicConstants.assignedStructureCountPerParent)));
    assignedStructureCountPerParent.toStream()
        .print(Printed.<String, Long>toSysOut());

    return locationImportStream;
  }


  private KStream<String, HierarchyStructure> getHierarchyStructureKStream(
      KStream<String, LocationAfterJoin> assignedStructuresWithParentKeyed) {
    KStream<String, HierarchyStructure> hierarchyStructuresStream = assignedStructuresWithParentKeyed
        .flatMapValues((k, v) -> {
          List<HierarchyStructure> hierarchyStructures = new ArrayList<>();
          if (v != null) {
            String locationId = v.getLocationIdentifier().toString();
            boolean isAssigned = v.isAssigned();
            String planIdentifier = v.getPlanIdentifier();
            hierarchyStructures = v.getAncestry().stream()
                .map(locationAncestor -> {
                  HierarchyStructure hierarchyStructure = new HierarchyStructure();
                  hierarchyStructure.setStructureId(locationId);
//                  hierarchyStructure.setParentId(locationAncestor.getLocationId());
                  hierarchyStructure.setAssigned(isAssigned);
                  hierarchyStructure.setPlanIdentifier(planIdentifier);
                  hierarchyStructure.setLocationAncestor(locationAncestor);
                  return hierarchyStructure;
                }).collect(Collectors.toList());
          }
          return hierarchyStructures;
        }).selectKey((k, v) -> v.getStructureId().concat("_")
            .concat(v.getLocationAncestor().getLocationId().concat("_").concat(v.getPlanIdentifier())))
        .mapValues((k, v) -> v.isAssigned() ? v : null);
    return hierarchyStructuresStream;
  }

  private KStream<String, HierarchyStructure> getStringHierarchyStructureKStream(
      KStream<String, LocationRelationshipMessage> assignedStructuresWithParentKeyed) {
    KStream<String, HierarchyStructure> hierarchyStructuresStream = assignedStructuresWithParentKeyed
        .flatMapValues((k, v) -> {
          List<HierarchyStructure> hierarchyStructures = new ArrayList<>();
          if (v != null) {
            String locationId = v.getLocationIdentifier().toString();
            hierarchyStructures = v.getLocationAncestors().stream()
                .map(locationAncestor -> {
                  HierarchyStructure hierarchyStructure = new HierarchyStructure();
                  hierarchyStructure.setStructureId(locationId);
                  hierarchyStructure.setLocationAncestor(locationAncestor);
                  return hierarchyStructure;
                }).collect(Collectors.toList());

          }
          return hierarchyStructures;
        }).selectKey((k, v) -> v.getStructureId().concat("_").concat(v.getLocationAncestor().getLocationId()));
    return hierarchyStructuresStream;
  }

  private List<LocationAssigned> getStructuresAssignedAndUnAssigned(
      PlanLocationAssignMessage planLocationAssignMessage) {

    List<LocationAssigned> locations = new ArrayList<>();
    String planIdentifier = planLocationAssignMessage.getPlanIdentifier();

    Plan plan = planService.getPlanByIdentifier(UUID.fromString(planIdentifier));

    if (planLocationAssignMessage.getLocationsAdded() != null) {

      List<LocationAssigned> locationsAssigned = planLocationAssignMessage.getLocationsAdded()
          .stream().flatMap(locationAdded -> {
            List<LocationProjection> locationProjections =
                locationRelationshipService.getLocationsHigherGeographicLevelsByLocationAndGeographicNameAndHierarchy(
                    UUID.fromString(locationAdded), LocationConstants.STRUCTURE,
                    plan.getLocationHierarchy().getIdentifier());
            return locationProjections.stream();
          }).map(LocationProjection::getIdentifier)
          .map(locationIdentifier -> new LocationAssigned(locationIdentifier, true, planIdentifier))
          .collect(Collectors.toList());
      locations.addAll(locationsAssigned);
    }

    if (planLocationAssignMessage.getLocationsRemoved() != null) {
      List<LocationAssigned> locationsUnAssigned = planLocationAssignMessage.getLocationsRemoved()
          .stream().flatMap(locationAdded -> {
            List<LocationProjection> locationProjections =
                locationRelationshipService.getLocationsHigherGeographicLevelsByLocationAndGeographicNameAndHierarchy(
                    UUID.fromString(locationAdded), LocationConstants.STRUCTURE,
                    plan.getLocationHierarchy().getIdentifier());
            return locationProjections.stream();
          }).map(LocationProjection::getIdentifier)
          .map(
              locationIdentifier -> new LocationAssigned(locationIdentifier, false, planIdentifier))
          .collect(Collectors.toList());
      locations.addAll(locationsUnAssigned);
    }
    return locations;
  }
}
