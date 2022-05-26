package com.revealprecision.revealserver.messaging.streams;

import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.LocationAssigned;
import com.revealprecision.revealserver.messaging.message.LocationBusinessStatus;
import com.revealprecision.revealserver.messaging.message.LocationMetadataContainer;
import com.revealprecision.revealserver.messaging.message.LocationMetadataEvent;
import com.revealprecision.revealserver.messaging.message.LocationRelationshipMessage;
import com.revealprecision.revealserver.messaging.message.OperationalAreaAggregate;
import com.revealprecision.revealserver.messaging.message.OperationalAreaVisitedCount2;
import com.revealprecision.revealserver.messaging.message.OperationalAreaVisitedCount2.OperationalAreaVisitedCount3;
import com.revealprecision.revealserver.messaging.message.PlanLocationAssignMessage;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.PlanService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
                  LocationRelationshipMessage locationRelationshipMessage1 = new LocationRelationshipMessage();
                  locationRelationshipMessage1.setLocationName(
                      locationRelationshipMessage.getLocationName());
                  locationRelationshipMessage1.setGeoName(locationRelationshipMessage.getGeoName());
                  locationRelationshipMessage1.setAncestor(ancestor);
                  locationRelationshipMessage1.setLocationIdentifier(
                      locationRelationshipMessage.getLocationIdentifier());
                  locationRelationshipMessage1.setLocationHierarchyIdentifier(
                      locationRelationshipMessage.getLocationHierarchyIdentifier());
                  return locationRelationshipMessage1;
                }).collect(Collectors.toList()))
        .selectKey((k, locationRelationshipMessage) ->
            locationRelationshipMessage.getLocationHierarchyIdentifier() + "_"
                + locationRelationshipMessage.getAncestor());

    structures.groupByKey().count(Materialized.as(kafkaProperties.getStoreMap()
        .get(KafkaConstants.structureCountPerParent)));

    return locationsImported;
  }

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

  //  @Bean
//  KStream<String, Long> getOperationalAreaCounts(StreamsBuilder streamsBuilder) {
//
//    KStream<String, Long> planStructureCounts = streamsBuilder.stream(
//        kafkaProperties.getTopicMap().get(KafkaConstants.LOCATION_BUSINESS_STATUS_COUNTS),
//        Consumed.with(Serdes.String(), Serdes.Long()));
////    planStructureCounts.print(Printed.<String, Long>toSysOut());
//
//    KStream<String, LocationAssigned> stringLocationAssignedKStream = planStructureCounts
//        .filter((k, v) -> locationService.findByIdentifier(UUID.fromString(k.split("_")[1]))
//            .getGeographicLevel().getName().equals("operational"))
//        .filter((k,v)->k.split("_")[3].equals("Not Visited"))
//        .mapValues((k, v) -> {
//          Plan plan = planService.getPlanByIdentifier(UUID.fromString(k.split("_")[0]));
//
//          LocationRelationship locationRelationshipsForLocation = locationRelationshipService.getLocationRelationshipsForLocation(
//              plan.getLocationHierarchy().getIdentifier(), UUID.fromString(k.split("_")[1]));
//          LocationAssigned locationAssigned = new LocationAssigned();
//          locationAssigned.setAncestry(locationRelationshipsForLocation.getAncestry());
//          locationAssigned.setIdentifier(k.split("_")[1]);
//          locationAssigned.setPlanIdentifier(plan.getIdentifier().toString());
//          locationAssigned.setCount(v);
//          return locationAssigned;
//        }).mapValues((k, v) -> {
//
//          String key = k.split("_")[0] + "_" +  //plan
//              k.split("_")[1] ;  //ancestor
//
//          KafkaStreams kafkaStreams = getKafkaStreams.getKafkaStreams();
//          ReadOnlyKeyValueStore<String, Long> counts = kafkaStreams.store(
//              StoreQueryParameters.fromNameAndType(kafkaProperties.getStoreMap().get(KafkaConstants.assignedStructureCountPerParent), QueryableStoreTypes.keyValueStore())
//          );
//
//
//          if (counts.get(key) !=null) {
//            Long countOfNotVisited = v.getCount();
//            Long countOfAssigned = counts.get(key);
//
//            if (countOfAssigned.doubleValue() > 0) {
//              double percentageOfNotVisited =
//                  countOfNotVisited.doubleValue() / countOfAssigned.doubleValue() * 100;
//
//              log.info("{} countOfNotVisited/countOfAssigned {}/{} percentage: {}", key,
//                  countOfNotVisited, countOfAssigned, percentageOfNotVisited);
//
//              if (percentageOfNotVisited < 80) {
//                v.setVisited(true);
//              } else {
//                v.setVisited(false);
//              }
//            } else {
//              v.setVisited(false);
//            }
//          } else {
//            v.setVisited(false);
//          }
//          return v;
//        }).selectKey((k,v)->v.getPlanIdentifier()+"_"+v.getIdentifier());
////    stringLocationAssignedKStream.print(Printed.<String,LocationAssigned>toSysOut());
//
//    stringLocationAssignedKStream.to(kafkaProperties.getTopicMap().get(KafkaConstants.OPERATIONAL_AREA_COUNTS));
//
//    return planStructureCounts;
//  }
  @Bean
  KStream<UUID, LocationMetadataEvent> getOperationalAreaCounts2(StreamsBuilder streamsBuilder) {

    KStream<UUID, LocationMetadataEvent> locationMetadataStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(KafkaConstants.LOCATION_METADATA_UPDATE),
        Consumed.with(Serdes.UUID(), new JsonSerde<>(LocationMetadataEvent.class)));

    locationMetadataStream
        .flatMapValues((k, locationMetadata) -> locationMetadata.getMetaDataEvents().stream()
            .map(metaDataEvent -> new LocationMetadataContainer(locationMetadata.getEntityId(),
                metaDataEvent, null)).collect(
                Collectors.toList()))
        .selectKey((k, metadataEventContainer) -> metadataEventContainer.getLocationIdentifier())
//            .filter((k,metadataEventContainer)->metadataEventContainer.getMetaDataEvent().isActive())
        .filter((k, metadataEventContainer) -> metadataEventContainer.getMetaDataEvent().getType()
            .equals("business-status"))
        .filter(
            (k, metadataEventContainer) -> locationService.findByIdentifier(k).getGeographicLevel()
                .getName().equals("structure"))
        .mapValues((k, metadataEventContainer) -> {
          Plan plan = planService.getPlanByIdentifier(
              metadataEventContainer.getMetaDataEvent().getTagData().getMeta().getPlanId());
          LocationMetadataContainer locationMetadataContainer = new LocationMetadataContainer();
          locationMetadataContainer.setLocationIdentifier(
              metadataEventContainer.getLocationIdentifier());
          locationMetadataContainer.setMetaDataEvent(metadataEventContainer.getMetaDataEvent());
          locationMetadataContainer.setAncestry(
              locationRelationshipService.getLocationRelationshipsForLocation(
                  plan.getLocationHierarchy().getIdentifier(),
                  metadataEventContainer.getLocationIdentifier()).getAncestry());
          return locationMetadataContainer;
        })
        .groupBy(
            (k, metadataEventContainer) -> metadataEventContainer.getLocationIdentifier().toString()
                .concat("_")
                .concat(metadataEventContainer.getMetaDataEvent().getTagData().getMeta().getPlanId()
                    .toString()))
        .aggregate(LocationBusinessStatus::new,
            (key, metadataEventContainer, aggregate) -> {
              if (metadataEventContainer.getMetaDataEvent().isActive()) {
                if (aggregate.getUpdateTime() == null) {
                  aggregate.setUpdateTime(
                      metadataEventContainer.getMetaDataEvent().getTagData().getMeta()
                          .getUpdateDateTime());
                  aggregate.setBusinessStatus(
                      metadataEventContainer.getMetaDataEvent().getTagData().getValue()
                          .getValueString());
                  aggregate.setEntityId(metadataEventContainer.getLocationIdentifier());
                  aggregate.setAncestry(metadataEventContainer.getAncestry());
                } else {
                  if (aggregate.getUpdateTime().isBefore(
                      metadataEventContainer.getMetaDataEvent().getTagData().getMeta()
                          .getUpdateDateTime())) {
                    aggregate.setUpdateTime(
                        metadataEventContainer.getMetaDataEvent().getTagData().getMeta()
                            .getUpdateDateTime());
                    aggregate.setBusinessStatus(
                        metadataEventContainer.getMetaDataEvent().getTagData().getValue()
                            .getValueString());
                    aggregate.setEntityId(metadataEventContainer.getLocationIdentifier());
                    aggregate.setAncestry(metadataEventContainer.getAncestry());
                  }
                }
              } else {
                aggregate = null;
              }
              return aggregate;
            }, Materialized.<String, LocationBusinessStatus, KeyValueStore<Bytes, byte[]>>as(
                    kafkaProperties.getStoreMap().get(KafkaConstants.locationBusinessStatusForOperationalAreas))
                .withValueSerde(new JsonSerde<>(LocationBusinessStatus.class))
                .withKeySerde(Serdes.String()))
        .groupBy((key, locationBusinessStatus) ->
                KeyValue.pair(locationBusinessStatus.getAncestry().stream().map(UUID::toString)
                        .collect(Collectors.joining("_"))+"_"+key.split("_")[key.split("_").length -1]
                    , locationBusinessStatus),
            Grouped.with(Serdes.String(), new JsonSerde<>(LocationBusinessStatus.class)))
        .aggregate(OperationalAreaAggregate::new,
            (key, locationBusinessStatus, aggregate) -> {
              Map<UUID, String> operationalAreaMap = aggregate.getAggregate();
              operationalAreaMap.put(locationBusinessStatus.getEntityId(),
                  locationBusinessStatus.getBusinessStatus());
              aggregate.setAggregate(operationalAreaMap);
              return aggregate;
            },
            (key, locationBusinessStatus, aggregate) -> {
              Map<UUID, String> operationalAreaMap = aggregate.getAggregate();
              operationalAreaMap.remove(locationBusinessStatus.getEntityId());
              aggregate.setAggregate(operationalAreaMap);
              return aggregate;
            },
            Materialized.<String, OperationalAreaAggregate, KeyValueStore<Bytes, byte[]>>as(
                    kafkaProperties.getStoreMap().get(KafkaConstants.tableOfOperationalAreas))
                .withValueSerde(new JsonSerde<>(OperationalAreaAggregate.class))
                .withKeySerde(Serdes.String())
        )
        .toStream()
        .flatMapValues((k, operationalAreaAggregate) -> {
          List<String> ancestry = new ArrayList<>(List.of(k.split("_")));
          String remove = ancestry.remove(0);
          List<OperationalAreaAggregate> collect = ancestry.stream().map(ancestor -> {
            OperationalAreaAggregate operationalAreaAggregateConstructed = new OperationalAreaAggregate();
            operationalAreaAggregateConstructed.setAggregate(
                operationalAreaAggregate.getAggregate());
            operationalAreaAggregateConstructed.setAncestorIdentifier(UUID.fromString(ancestor));
            operationalAreaAggregateConstructed.setIdentifier(UUID.fromString(remove));
            return operationalAreaAggregateConstructed;
          }).collect(Collectors.toList());
          return collect;
        })
//         .peek((k,operationalAreaAggregate) -> log.info("k: {},v:{}",k,operationalAreaAggregate))
        .groupBy(
            (k, operationalAreaAggregate) -> operationalAreaAggregate.getAncestorIdentifier() + "_"
                + operationalAreaAggregate.getIdentifier()+"_"+k.split("_")[k.split("_").length -1])

        .aggregate(OperationalAreaAggregate::new,
            (key, operationalAreaAggregate, aggregate) -> {

              Map<String, Set<UUID>> locationMap = new HashMap<>();

              Set<Entry<UUID, String>> entries = operationalAreaAggregate.getAggregate().entrySet();

              for (Entry<UUID, String> entry : entries) {
                if (locationMap.containsKey(entry.getValue())) {
                  locationMap.get(entry.getValue()).add(entry.getKey());
                } else {
                  Set<UUID> uuids = new HashSet<>();
                  uuids.add(entry.getKey());
                  locationMap.put(entry.getValue(), uuids);
                }
              }
              aggregate.setAggregatedLocations(locationMap);

              Set<Entry<String, Set<UUID>>> entries1 = aggregate.getAggregatedLocations()
                  .entrySet();

              Map<String, Long> stringLongMap = new HashMap<>();
              for (Entry<String, Set<UUID>> entry : entries1) {
                stringLongMap.put(entry.getKey(), (long) entry.getValue().size());
              }
              aggregate.setAggregatedLocationCount(stringLongMap);

              return aggregate;
            },
            Materialized.<String, OperationalAreaAggregate, KeyValueStore<Bytes, byte[]>>as(
                    kafkaProperties.getStoreMap().get(KafkaConstants.tableOfOperationalAreaHierarchies))
                .withValueSerde(new JsonSerde<>(OperationalAreaAggregate.class))
                .withKeySerde(Serdes.String()))
        .toStream()
        .mapValues((k, operationalAreaAggregate) -> {
          operationalAreaAggregate.setIdentifier(UUID.fromString(k.split("_")[1]));
          return operationalAreaAggregate;
        })
//        .mapValues(
//            (k, operationalAreaAggregate) -> operationalAreaAggregate.getAggregatedLocationCount().isEmpty()
//                ? null : operationalAreaAggregate)
//        .flatMapValues((k,operationalAreaAggregate) -> new ArrayList<>(operationalAreaAggregate.getAggregatedLocationCount().entrySet()))
        .groupBy((k, entry) -> k.split("_")[0]+"_"+k.split("_")[k.split("_").length -1])
        .aggregate((OperationalAreaVisitedCount2::new),
            (k, operationalAreaAggregate, aggregate) -> {

              Long totalStructures = operationalAreaAggregate.getAggregatedLocationCount().values()
                  .stream().reduce(0L, Long::sum);
              Long notVisitedStructures = operationalAreaAggregate.getAggregatedLocationCount().entrySet()
                  .stream().filter(entry -> entry.getKey().equals("Not Visited")).map(Entry::getValue).reduce(0L, Long::sum);
              boolean operationalAreaIsVisited = false;
              if (totalStructures > 0) {
                if (notVisitedStructures / totalStructures * 100 < 20) {
                  operationalAreaIsVisited = true;
                }
              }

              if (aggregate.getOperationalObj().containsKey(operationalAreaAggregate.getIdentifier())){
                OperationalAreaVisitedCount3 operationalAreaVisitedCount3 = aggregate.getOperationalObj()
                    .get(operationalAreaAggregate.getIdentifier());
                operationalAreaVisitedCount3.setCounts(operationalAreaAggregate.getAggregatedLocationCount());
                operationalAreaVisitedCount3.setOperationalAreaIsVisited(operationalAreaIsVisited);
                aggregate.getOperationalObj().put(operationalAreaAggregate.getIdentifier(),operationalAreaVisitedCount3);

              }else{
                OperationalAreaVisitedCount3 operationalAreaVisitedCount3 = new OperationalAreaVisitedCount3();
                operationalAreaVisitedCount3.setCounts(operationalAreaAggregate.getAggregatedLocationCount());
                operationalAreaVisitedCount3.setOperationalAreaIsVisited(operationalAreaIsVisited);
                aggregate.getOperationalObj().put(operationalAreaAggregate.getIdentifier(),operationalAreaVisitedCount3);
              }

              long countOfVisitedOperationalAreas = aggregate.getOperationalObj().entrySet().stream()
                  .filter(entry -> entry.getValue().isOperationalAreaIsVisited()).count();
              aggregate.setOperationalAreaVisitedCount(countOfVisitedOperationalAreas);

              return aggregate;
            },
            Materialized.<String, OperationalAreaVisitedCount2, KeyValueStore<Bytes, byte[]>>as(
                    kafkaProperties.getStoreMap().get(KafkaConstants.operationalAreaByPlanParentHierarchy))
                .withValueSerde(new JsonSerde<>(OperationalAreaVisitedCount2.class))
                .withKeySerde(Serdes.String()));

//            (k, metadataEventContainer, operationalAreaAggregate) -> {
//
//              String newBusinessStatus = metadataEventContainer.getMetaDataEvent().getTagData().getValue()
//                  .getValueString();
//              if (operationalAreaAggregate.getAggregate() ==  null){
//                Map<String, LocationCountContainer> locationCountContainerHashMap = new HashMap<>();
//
//                LocationCountContainer locationCountContainer = new LocationCountContainer();
//                locationCountContainer.setCount(1L);
//
//                List<UUID> locationUiids = new ArrayList<>();
//                locationUiids.add(metadataEventContainer.getLocationIdentifier());
//                locationCountContainer.setLocationIdentifiers(locationUiids);
//
//                Map<UUID,String> locationBusinessStatusMap = new HashMap<>();
//                locationBusinessStatusMap.put(metadataEventContainer.getLocationIdentifier()
//                    ,newBusinessStatus);
//
//                locationCountContainerHashMap.put(newBusinessStatus, locationCountContainer);
//
//                operationalAreaAggregate.setAggregate(locationCountContainerHashMap);
//                operationalAreaAggregate.setLocationBusinessStatusMap(locationBusinessStatusMap);
//              }else{
//
//                if (operationalAreaAggregate.getAggregate().containsKey(newBusinessStatus)){
//                  Map<String, LocationCountContainer> businessStatusCountMap = operationalAreaAggregate.getAggregate();
//
//                  Map<UUID,String> locationBusinessStatusMap = operationalAreaAggregate.getLocationBusinessStatusMap();
//
//                  if(locationBusinessStatusMap.containsKey(metadataEventContainer.getLocationIdentifier())) {
//                    String locationsCurrentBusinessStatus = locationBusinessStatusMap.get(
//                        metadataEventContainer.getLocationIdentifier());
//
//                    if (!locationsCurrentBusinessStatus.equals(newBusinessStatus)) {
//                      LocationCountContainer currentLocationCountContainer = businessStatusCountMap.get(
//                          locationsCurrentBusinessStatus);
//
//                      currentLocationCountContainer.getLocationIdentifiers()
//                          .remove(metadataEventContainer.getLocationIdentifier());
//
//                      currentLocationCountContainer.setCount(
//                          currentLocationCountContainer.getCount() - 1);
//                      businessStatusCountMap.put(locationsCurrentBusinessStatus,
//                          currentLocationCountContainer);
//
//                      LocationCountContainer newLocationCountContainer = businessStatusCountMap.get(
//                          newBusinessStatus);
//                      newLocationCountContainer.setCount(newLocationCountContainer.getCount() + 1);
//                      newLocationCountContainer.getLocationIdentifiers()
//                          .add(metadataEventContainer.getLocationIdentifier());
//                      businessStatusCountMap.put(newBusinessStatus, newLocationCountContainer);
//                    }
//                  }else{
//
//                  }
//
//                  operationalAreaAggregate.setAggregate(businessStatusCountMap);
//
//                }else{
//                  Map<String, LocationCountContainer> businessStatusCountMap = operationalAreaAggregate.getAggregate();
//                  businessStatusCountMap.put(newBusinessStatus,1L);
//                  operationalAreaAggregate.setAggregate(businessStatusCountMap);
//                }
//              }
//              return operationalAreaAggregate;
//            },
//            Materialized.<String, OperationalAreaAggregate, KeyValueStore<Bytes, byte[]>>as(
//                    kafkaProperties.getStoreMap().get(KafkaConstants.locationBusinessStatus))
//                .withValueSerde(new JsonSerde<>(OperationalAreaAggregate.class))
//                .withKeySerde(Serdes.String())
//            );

    return locationMetadataStream;
  }

  public static void main(String[] args) {
    Long a = 0L;

    if (a.doubleValue() > 0) {
      System.out.println("true");
    } else {
      System.out.println("false");
    }
  }

//  @Bean("OperationalAreaTable")
//  KStream<String, LocationAssigned> getOperationalAreaTable(StreamsBuilder streamsBuilder) {
//
//   KTable<String,LocationAssigned> operationalAreaTable = streamsBuilder.table(kafkaProperties.getTopicMap().get(KafkaConstants.OPERATIONAL_AREA_COUNTS),
//            Consumed.with(Serdes.String(), new JsonSerde<>(LocationAssigned.class))
//           , Materialized.as(kafkaProperties.getStoreMap()
//               .get(KafkaConstants.tableOfOperationalAreas)));
//
//    KStream<String, LocationAssigned> stringLocationAssignedKStream1 = operationalAreaTable.toStream();
//
//    KStream<String, LocationAssigned> stringLocationAssignedKStream = stringLocationAssignedKStream1.flatMapValues(
//        (k, v) -> {
//          return v.getAncestry().stream().map(ancestor -> {
//            LocationAssigned locationAssigned = new LocationAssigned();
//            locationAssigned.setAssigned(v.isAssigned());
//            locationAssigned.setVisited(v.isVisited());
//            locationAssigned.setAncestor(ancestor);
//            locationAssigned.setPlanIdentifier(v.getPlanIdentifier());
//            locationAssigned.setIdentifier(v.getIdentifier());
//            return locationAssigned;
//          }).collect(Collectors.toList());
//        }).selectKey((k,v)->{
//          return v.getPlanIdentifier()+"_"+v.getIdentifier()+"_"+v.getAncestor();
//    });
//
//    KTable<String, OperationalAreaVisitedCount> count = stringLocationAssignedKStream
//        .mapValues((k, v) -> v.isVisited() ? v : null)
//        .groupBy((k, v) ->
//                k.split("_")[0] + "_" + k.split("_")[2]
//            , Grouped.with(Serdes.String(), new JsonSerde<>(LocationAssigned.class)))
//            .aggregate(OperationalAreaVisitedCount::new,
//                (k,v,agg)->{
//                    if (agg.getCount() != null){
//                      if (!agg.getListOfKeys().contains(v.getPlanIdentifier()+"_"+v.getIdentifier()+"_"+v.getAncestor())){
//                        List<String> listOfKeys = new ArrayList<>(agg.getListOfKeys());
//                        listOfKeys.add(v.getPlanIdentifier()+"_"+v.getIdentifier()+"_"+v.getAncestor());
//                        agg.setListOfKeys(listOfKeys);
//                        agg.setCount(agg.getCount()+1);
//                      }
//                    } else {
//                      agg.setCount(1L);
//                      agg.setListOfKeys(List.of(v.getPlanIdentifier()+"_"+v.getIdentifier()+"_"+v.getAncestor()));
//                    }
//                    return agg;
//                },Materialized.<String,OperationalAreaVisitedCount, KeyValueStore<Bytes, byte[]>>as(
//            kafkaProperties.getStoreMap().get(KafkaConstants.tableOfOperationalAreaHierarchies))
//                    .withKeySerde(Serdes.String())
//                    .withValueSerde(new JsonSerde<>(OperationalAreaVisitedCount.class)));
//    return stringLocationAssignedKStream1;
//  }


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
