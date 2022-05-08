package com.revealprecision.revealserver.messaging.streams;

import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.LocationBusinessStatus;
import com.revealprecision.revealserver.messaging.message.LocationMetadataEvent;
import com.revealprecision.revealserver.messaging.message.LocationMetadataUnpackedEvent;
import com.revealprecision.revealserver.messaging.message.TaskAggregate;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.LocationHierarchyService;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.LocationService;
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
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class LocationBusinessStatusStream {


  private final KafkaProperties kafkaProperties;
  private final LocationService locationService;
  private final LocationRelationshipService locationRelationshipService;
  private final LocationHierarchyService locationHierarchyService;

  @Bean
  KStream<UUID, LocationMetadataEvent> getLocationsRelationships2(StreamsBuilder streamsBuilder) {

    //locations imported
    KStream<UUID, LocationMetadataEvent> locationMetadataStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(KafkaConstants.LOCATION_METADATA_UPDATE),
        Consumed.with(Serdes.UUID(), new JsonSerde<>(LocationMetadataEvent.class)));
//    locationMetadataStream.print(Printed.<UUID, LocationMetadataEvent>toSysOut());

    KStream<String, LocationMetadataUnpackedEvent> unpackedLocationMetadataStream = locationMetadataStream
        .flatMapValues((k, locationMetadata) -> {
          List<LocationMetadataEvent> locationMetadataEventsWithHierarchy = locationHierarchyService.getAll()
              .stream()
              .map(locationHierarchy -> {
                LocationMetadataEvent locationMetadataEvent = new LocationMetadataEvent();
                locationMetadataEvent.setHierarchyIdentifier(locationHierarchy.getIdentifier());
                locationMetadataEvent.setMetaDataEvents(locationMetadata.getMetaDataEvents());
                locationMetadataEvent.setEntityId(locationMetadata.getEntityId());
                return locationMetadataEvent;
              }).collect(Collectors.toList());
          return locationMetadataEventsWithHierarchy;
        })
        .flatMapValues((k, locationMetadata) -> {
          LocationRelationship locationRelationShip = locationRelationshipService.getLocationRelationshipsForLocation(
              locationMetadata.getHierarchyIdentifier(), locationMetadata.getEntityId());
          List<LocationMetadataEvent> locationMetadataEventsWithHierarchyAndAncestorNode = locationRelationShip.getAncestry()
              .stream()
              .map(ancestorNode -> {
                LocationMetadataEvent locationMetadataEvent = new LocationMetadataEvent();
                locationMetadataEvent.setHierarchyIdentifier(
                    locationMetadata.getHierarchyIdentifier());
                locationMetadataEvent.setMetaDataEvents(locationMetadata.getMetaDataEvents());
                locationMetadataEvent.setEntityId(locationMetadata.getEntityId());
                locationMetadataEvent.setAncestorNode(ancestorNode);
                return locationMetadataEvent;
              }).collect(Collectors.toList());
          return locationMetadataEventsWithHierarchyAndAncestorNode;
        })
        .flatMapValues((k, locationMetadata) -> {
          List<LocationMetadataUnpackedEvent> locationMetadataEventsWithHierarchyAndAncestorNodeWithIndividualMetadata = locationMetadata.getMetaDataEvents()
              .stream().map(metaDataEvent -> {
                LocationMetadataUnpackedEvent locationMetadataUnpackedEvent = new LocationMetadataUnpackedEvent();
                locationMetadataUnpackedEvent.setHierarchyIdentifier(
                    locationMetadata.getHierarchyIdentifier());
                locationMetadataUnpackedEvent.setEntityId(locationMetadata.getEntityId());
                locationMetadataUnpackedEvent.setAncestorNode(locationMetadata.getAncestorNode());
                locationMetadataUnpackedEvent.setMetaDataEvent(metaDataEvent);
                return locationMetadataUnpackedEvent;
              }).collect(Collectors.toList());
          return locationMetadataEventsWithHierarchyAndAncestorNodeWithIndividualMetadata;
        }).selectKey((k, v) -> {
          return v.getMetaDataEvent().getTagData().getMeta().getPlanId() + "_" +
              v.getAncestorNode() + "_" +
              v.getHierarchyIdentifier() + "_" +
              v.getEntityId() //+ "_" +
              //v.getMetaDataEvent().getTagData().getMeta().getTaskId() + "_" +
              //v.getMetaDataEvent().getTagData().getValue().getValueString().toLowerCase().replace(" ",".")
              ;
        });
//    unpackedLocationMetadataStream.print(Printed.<String, LocationMetadataUnpackedEvent>toSysOut());

//    unpackedLocationMetadataStream.to(
//        kafkaProperties.getTopicMap().get(KafkaConstants.PLAN_LOCATION_HIERARCHY_PARENT));
//
//    KTable<String, LocationMetadataUnpackedEvent> planLocationHierarchyParentTable = streamsBuilder.table(
//        kafkaProperties.getTopicMap().get(KafkaConstants.PLAN_LOCATION_HIERARCHY_PARENT),Consumed.with(Serdes.String(),new JsonSerde<>(LocationMetadataUnpackedEvent.class)));
//
//
//    planLocationHierarchyParentTable.((v, locationMetadataUnpackedEvent) -> {
//          return locationMetadataUnpackedEvent.getMetaDataEvent().getTagData().getMeta().getPlanId() + "_" +
//              locationMetadataUnpackedEvent.getAncestorNode() + "_" +
//              locationMetadataUnpackedEvent.getHierarchyIdentifier() + "_" +
//              locationMetadataUnpackedEvent.getEntityId();
//        })
//        .aggregate((v, locationMetadataUnpackedEvent) -> {
//
//        })
    KGroupedStream<String, LocationMetadataUnpackedEvent> stringLocationMetadataUnpackedEventKGroupedStream = unpackedLocationMetadataStream
        .selectKey((k,v)->k)
        .groupByKey(
        Grouped.with(Serdes.String(), new JsonSerde<>(LocationMetadataUnpackedEvent.class)));

    KTable<String, LocationBusinessStatus> aggregate1 = stringLocationMetadataUnpackedEventKGroupedStream
        .aggregate(LocationBusinessStatus::new,
            (key, locationMetadataUnpackedEvent, aggregate) -> {
              if (aggregate.getUpdateTime() == null) {
                aggregate.setUpdateTime(
                    locationMetadataUnpackedEvent.getMetaDataEvent().getTagData().getMeta()
                        .getUpdateDateTime());
                aggregate.setBusinessStatus(
                    locationMetadataUnpackedEvent.getMetaDataEvent().getTagData().getValue()
                        .getValueString());
                aggregate.setEntityId(locationMetadataUnpackedEvent.getEntityId());
              } else {
                if (aggregate.getUpdateTime().isBefore(
                    locationMetadataUnpackedEvent.getMetaDataEvent().getTagData().getMeta()
                        .getUpdateDateTime())) {
                  aggregate.setUpdateTime(
                      locationMetadataUnpackedEvent.getMetaDataEvent().getTagData().getMeta()
                          .getUpdateDateTime());
                  aggregate.setBusinessStatus(
                      locationMetadataUnpackedEvent.getMetaDataEvent().getTagData().getValue()
                          .getValueString());
                  aggregate.setEntityId(locationMetadataUnpackedEvent.getEntityId());
                }
              }
              return aggregate;
            },
            Materialized.<String, LocationBusinessStatus, KeyValueStore<Bytes, byte[]>>as(kafkaProperties.getStoreMap().get(KafkaConstants.locationBusinessStatus))
                .withValueSerde(new JsonSerde<>(LocationBusinessStatus.class))
                .withKeySerde(Serdes.String())
        );
//    aggregate1.toStream().print(Printed.<String,LocationBusinessStatus>toSysOut());

    KTable<String, Long> count = aggregate1.groupBy((k, v) ->
            KeyValue.pair(k.split("_")[0] + "_" +
                k.split("_")[1] + "_" +
                k.split("_")[2] + "_" +
                v.getBusinessStatus(), v),
        Grouped.with(Serdes.String(), new JsonSerde<>(LocationBusinessStatus.class))
    ).count(Materialized.as(kafkaProperties.getStoreMap().get(KafkaConstants.locationBusinessStatusByPlanParentHierarchy)));

    count.toStream().print(Printed.<String,Long>toSysOut());

//    KTable<String, LocationRelationshipMessage> structuresImportedTable = streamsBuilder.table(
//        kafkaProperties.getTopicMap().get(STRUCTURES_IMPORTED_FOR_BUSINESS_STATUS),
//        Consumed.with(Serdes.String(), new JsonSerde<>(LocationRelationshipMessage.class)));
//
//    KStream<String, LocationMetadataUnpackedEventWithAncestorLocationList> stringLocationMetadataUnpackedEventKStream = unpackedLocationMetadataStream.join(
//        structuresImportedTable, (locationMetadataUnpackedEvent, locationRelationshipMessage) -> {
//          LocationMetadataUnpackedEventWithAncestorLocationList
//              unpackedEventWithAncestorLocationList = new LocationMetadataUnpackedEventWithAncestorLocationList();
//          unpackedEventWithAncestorLocationList.setIdentifier(
//              locationMetadataUnpackedEvent.getIdentifier());
//          unpackedEventWithAncestorLocationList.setMetaDataEvent(
//              locationMetadataUnpackedEvent.getMetaDataEvent());
//          unpackedEventWithAncestorLocationList.setLocationAncestors(
//              locationRelationshipMessage.getLocationAncestors());
//          return unpackedEventWithAncestorLocationList;
//        });
//    stringLocationMetadataUnpackedEventKStream.print(Printed.<String, LocationMetadataUnpackedEventWithAncestorLocationList>toSysOut());
//
//    KStream<String, LocationMetadataUnpackedEventWithIndividualAncestorLocation> locationMetaDataWithLocationAncestorStream = stringLocationMetadataUnpackedEventKStream.flatMapValues(
//        (k, locationMetadataUnpackedEventWithAncestorLocationList) -> locationMetadataUnpackedEventWithAncestorLocationList.getLocationAncestors()
//            .stream()
//            .map(locationAncestor -> {
//              LocationMetadataUnpackedEventWithIndividualAncestorLocation metadataObj = new LocationMetadataUnpackedEventWithIndividualAncestorLocation();
//              metadataObj.setIdentifier(
//                  locationMetadataUnpackedEventWithAncestorLocationList.getIdentifier());
//              metadataObj.setMetaDataEvent(
//                  locationMetadataUnpackedEventWithAncestorLocationList.getMetaDataEvent());
//              metadataObj.setLocationAncestor(locationAncestor);
//              return metadataObj;
//            }).collect(Collectors.toList()))
//            .selectKey((k,locationMetadataUnpackedEventWithIndividualAncestorLocation)->
//                locationMetadataUnpackedEventWithIndividualAncestorLocation.getMetaDataEvent().getEntityId().toString() + "_" +
//                locationMetadataUnpackedEventWithIndividualAncestorLocation.getMetaDataEvent().getTagData().getMeta().getTaskId().toString());
//
//    locationMetaDataWithLocationAncestorStream.print(Printed.<String, LocationMetadataUnpackedEventWithIndividualAncestorLocation>toSysOut());
//    locationMetaDataWithLocationAncestorStream.to(kafkaProperties.getTopicMap().get(KafkaConstants.STRUCTURES_WITH_BUSINESS_STATUS_AND_ANCESTOR));
//
//    KTable<String, LocationMetadataUnpackedEventWithIndividualAncestorLocation> locationMetaDataWithLocationAncestorTable = streamsBuilder
//        .table(kafkaProperties.getTopicMap().get(KafkaConstants.STRUCTURES_WITH_BUSINESS_STATUS_AND_ANCESTOR)
//            ,Consumed.with(Serdes.String(),new JsonSerde<>(LocationMetadataUnpackedEventWithIndividualAncestorLocation.class)),
//            Materialized.as(kafkaProperties.getStoreMap().get(KafkaConstants.getStructuresWithBusinessStatusAndAncestor)));
//
//    KTable<String, Long> countOfBusinessStatusByTaskIdBusinessStatusPlanIdAncestorId = locationMetaDataWithLocationAncestorTable.toStream()
//        .groupBy(
//        (k, locationMetadataUnpackedEventWithIndividualAncestorLocation) ->
//            locationMetadataUnpackedEventWithIndividualAncestorLocation.getMetaDataEvent()
//                .getTagData().getMeta().getTaskId()+ "_" +
//            locationMetadataUnpackedEventWithIndividualAncestorLocation.getMetaDataEvent()
//                .getTagData().getValue().getValueString().toLowerCase().replace(" ",".") + "_"
//                + locationMetadataUnpackedEventWithIndividualAncestorLocation.getMetaDataEvent()
//                .getTagData().getMeta()
//                .getPlanId() + "_" +
//                locationMetadataUnpackedEventWithIndividualAncestorLocation.getLocationAncestor().getLocationId()).count(Materialized.as(
//        kafkaProperties.getStoreMap().get(KafkaConstants.businessStatusCount)));
//    countOfBusinessStatusByTaskIdBusinessStatusPlanIdAncestorId.toStream().print(Printed.<String, Long>toSysOut());

    return locationMetadataStream;
  }


}
