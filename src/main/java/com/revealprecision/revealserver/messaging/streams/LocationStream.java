package com.revealprecision.revealserver.messaging.streams;

import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.LocationAssigned;
import com.revealprecision.revealserver.messaging.message.OperationalAreaVisitedCount;
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
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class LocationStream {

  private final LocationService locationService;

  private final LocationRelationshipService locationRelationshipService;

  private final PlanService planService;

  private final KafkaProperties kafkaProperties;

  @Autowired
   StreamsBuilderFactoryBean getKafkaStreams;

  @Bean
  KStream<String, PlanLocationAssignMessage> getAssignedStructures(
      StreamsBuilder streamsBuilder) {

    // getting values from plan assignment
    KStream<String, PlanLocationAssignMessage> locationsAssignedStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(KafkaConstants.PLAN_LOCATION_ASSIGNED),
        Consumed.with(Serdes.String(), new JsonSerde<>(PlanLocationAssignMessage.class)));
//    locationsAssignedStream.print(Printed.<String, PlanLocationAssignMessage>toSysOut());

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
        .selectKey((key, locationAssigned) -> locationAssigned.getIdentifier() + "_"
            + locationAssigned.getPlanIdentifier() + "_" + locationAssigned.getAncestor())
        .mapValues((k, v) -> v.isAssigned() ? v : null);

    stringLocationAssignedKStream.to(
        kafkaProperties.getTopicMap().get(KafkaConstants.PLAN_STRUCTURES_ASSIGNED));

    KTable<String, LocationAssigned> tableOfAssignedStructures = streamsBuilder.table(
        kafkaProperties.getTopicMap().get(KafkaConstants.PLAN_STRUCTURES_ASSIGNED)
        , Consumed.with(Serdes.String(), new JsonSerde<>(LocationAssigned.class))
        , Materialized.as(kafkaProperties.getStoreMap()
            .get(KafkaConstants.tableOfAssignedStructuresWithParentKeyed)));

    KTable<String, Long> assignedStructureCountPerParent = tableOfAssignedStructures
        .groupBy((key, locationAssigned) -> KeyValue.pair(
                locationAssigned.getPlanIdentifier() + "_" + locationAssigned.getAncestor(),
                locationAssigned),
            Grouped.with(Serdes.String(), new JsonSerde<>(LocationAssigned.class)))
        .count(Materialized.as(
            kafkaProperties.getStoreMap().get(KafkaConstants.assignedStructureCountPerParent)));
//    assignedStructureCountPerParent.toStream()
//        .print(Printed.<String, Long>toSysOut());
    assignedStructureCountPerParent.toStream()
        .to(kafkaProperties.getTopicMap().get(KafkaConstants.PLAN_STRUCTURES_COUNTS));

    return locationsAssignedStream;
  }

  @Bean
  KStream<String, Long> getOperationalAreaCounts(StreamsBuilder streamsBuilder) {

    KStream<String, Long> planStructureCounts = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(KafkaConstants.LOCATION_BUSINESS_STATUS_COUNTS),
        Consumed.with(Serdes.String(), Serdes.Long()));
//    planStructureCounts.print(Printed.<String, Long>toSysOut());

    KStream<String, LocationAssigned> stringLocationAssignedKStream = planStructureCounts
        .filter((k, v) -> locationService.findByIdentifier(UUID.fromString(k.split("_")[1]))
            .getGeographicLevel().getName().equals("operational"))
        .filter((k,v)->k.split("_")[3].equals("Not Visited"))
        .mapValues((k, v) -> {
          Plan plan = planService.getPlanByIdentifier(UUID.fromString(k.split("_")[0]));

          LocationRelationship locationRelationshipsForLocation = locationRelationshipService.getLocationRelationshipsForLocation(
              plan.getLocationHierarchy().getIdentifier(), UUID.fromString(k.split("_")[1]));
          LocationAssigned locationAssigned = new LocationAssigned();
          locationAssigned.setAncestry(locationRelationshipsForLocation.getAncestry());
          locationAssigned.setIdentifier(k.split("_")[1]);
          locationAssigned.setPlanIdentifier(plan.getIdentifier().toString());
          locationAssigned.setCount(v);
          return locationAssigned;
        }).mapValues((k, v) -> {

          String key = k.split("_")[0] + "_" +  //plan
              k.split("_")[1] ;  //ancestor

          KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
          ReadOnlyKeyValueStore<String, Long> counts = kafkaStreams.store(
              StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap().get(KafkaConstants.assignedStructureCountPerParent), QueryableStoreTypes.keyValueStore())
          );

          if (counts.get(key) !=null) {
            Long countOfNotVisited = v.getCount();
            Long countOfAssigned = counts.get(key);

            double percentageOfNotVisited =
                countOfNotVisited.doubleValue() / countOfAssigned.doubleValue() * 100;

            log.info("{} countOfNotVisited/countOfAssigned {}/{} percentage: {}",key,countOfNotVisited,countOfAssigned,percentageOfNotVisited);

            if (percentageOfNotVisited < 80) {
              v.setVisited(true);
            } else {
              v.setVisited(false);
            }
          } else {
            v.setVisited(false);
          }
          return v;
        }).selectKey((k,v)->v.getPlanIdentifier()+"_"+v.getIdentifier());
//    stringLocationAssignedKStream.print(Printed.<String,LocationAssigned>toSysOut());

    stringLocationAssignedKStream.to(kafkaProperties.getTopicMap().get(KafkaConstants.OPERATIONAL_AREA_COUNTS));

    return planStructureCounts;
  }


  @Bean
  KStream<String, LocationAssigned> getOperationalAreaTable(StreamsBuilder streamsBuilder) {

   KTable<String,LocationAssigned> operationalAreaTable = streamsBuilder.table(kafkaProperties.getTopicMap().get(KafkaConstants.OPERATIONAL_AREA_COUNTS),
            Consumed.with(Serdes.String(), new JsonSerde<>(LocationAssigned.class))
           , Materialized.as(kafkaProperties.getStoreMap()
               .get(KafkaConstants.tableOfOperationalAreas)));

    KStream<String, LocationAssigned> stringLocationAssignedKStream1 = operationalAreaTable.toStream();

    KStream<String, LocationAssigned> stringLocationAssignedKStream = stringLocationAssignedKStream1.flatMapValues(
        (k, v) -> {
          return v.getAncestry().stream().map(ancestor -> {
            LocationAssigned locationAssigned = new LocationAssigned();
            locationAssigned.setAssigned(v.isAssigned());
            locationAssigned.setVisited(v.isVisited());
            locationAssigned.setAncestor(ancestor);
            locationAssigned.setPlanIdentifier(v.getPlanIdentifier());
            locationAssigned.setIdentifier(v.getIdentifier());
            return locationAssigned;
          }).collect(Collectors.toList());
        }).selectKey((k,v)->{
          return v.getPlanIdentifier()+"_"+v.getIdentifier()+"_"+v.getAncestor();
    });

    KTable<String, OperationalAreaVisitedCount> count = stringLocationAssignedKStream
        .mapValues((k, v) -> v.isVisited() ? v : null)
        .groupBy((k, v) ->
                k.split("_")[0] + "_" + k.split("_")[2]
            , Grouped.with(Serdes.String(), new JsonSerde<>(LocationAssigned.class)))
            .aggregate(OperationalAreaVisitedCount::new,
                (k,v,agg)->{
                    if (agg.getCount() != null){
                      if (!agg.getListOfKeys().contains(v.getPlanIdentifier()+"_"+v.getIdentifier()+"_"+v.getAncestor())){
                        List<String> listOfKeys = new ArrayList<>(agg.getListOfKeys());
                        listOfKeys.add(v.getPlanIdentifier()+"_"+v.getIdentifier()+"_"+v.getAncestor());
                        agg.setListOfKeys(listOfKeys);
                        agg.setCount(agg.getCount()+1);
                      }
                    } else {
                      agg.setCount(1L);
                      agg.setListOfKeys(List.of(v.getPlanIdentifier()+"_"+v.getIdentifier()+"_"+v.getAncestor()));
                    }
                    return agg;
                },Materialized.<String,OperationalAreaVisitedCount, KeyValueStore<Bytes, byte[]>>as(
            kafkaProperties.getStoreMap().get(KafkaConstants.tableOfOperationalAreaHierarchies))
                    .withKeySerde(Serdes.String())
                    .withValueSerde(new JsonSerde<>(OperationalAreaVisitedCount.class)));
    return stringLocationAssignedKStream1;
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
