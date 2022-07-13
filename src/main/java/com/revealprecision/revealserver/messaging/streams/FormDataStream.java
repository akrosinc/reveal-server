package com.revealprecision.revealserver.messaging.streams;

import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.LocationFormDataAggregateEvent;
import com.revealprecision.revealserver.messaging.message.LocationFormDataCountAggregateEvent;
import com.revealprecision.revealserver.messaging.message.LocationFormDataSumAggregateEvent;
import com.revealprecision.revealserver.messaging.message.LocationMetadataEvent;
import com.revealprecision.revealserver.messaging.message.LocationMetadataUnpackedEvent;
import com.revealprecision.revealserver.messaging.message.MetaDataEvent;
import com.revealprecision.revealserver.messaging.message.PersonFormDataAggregateEvent;
import com.revealprecision.revealserver.messaging.message.PersonFormDataCountAggregateEvent;
import com.revealprecision.revealserver.messaging.message.PersonMetadataEvent;
import com.revealprecision.revealserver.messaging.message.PersonMetadataUnpackedEvent;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.EntityTagService;
import com.revealprecision.revealserver.service.LocationHierarchyService;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KGroupedStream;
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
public class FormDataStream {

  private final KafkaProperties kafkaProperties;
  private final LocationRelationshipService locationRelationshipService;
  private final LocationHierarchyService locationHierarchyService;
  private final EntityTagService entityTagService;
  private final Logger streamLog = LoggerFactory.getLogger("stream-file");

  @Bean
  KStream<UUID, PersonMetadataEvent> personFormDataProcessor(StreamsBuilder streamsBuilder) {

    KStream<UUID, PersonMetadataEvent> personFormDataStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(KafkaConstants.PERSON_METADATA_UPDATE),
        Consumed.with(Serdes.UUID(), new JsonSerde<>(PersonMetadataEvent.class)));

    KStream<String, PersonMetadataUnpackedEvent> unpackedPersonFormDataStream = personFormDataStream
        .mapValues(
            (k, personFormData) -> getPersonMetadataUnpackedForHierarchy(personFormData))
        .flatMapValues(
            (k, personFormData) -> getUnpackedPersonMetadataPerAncestorList(personFormData))
        .flatMapValues(
            (k, personFormData) -> getUnpackedPersonMetadataPerMetaItemList(personFormData))
        .filter(
            (k, personFormData) -> personFormData.getMetaDataEvent().getType().equals("FormData"))
        .selectKey((k, personMetadata) -> getPlanAncestorHierarchyKey(personMetadata));

    unpackedPersonFormDataStream.peek(
        (k, v) -> streamLog.debug("unpackedPersonFormDataStream k: {} v: {}", k, v));

    //////////////////counting

    KGroupedStream<String, PersonMetadataUnpackedEvent> countStream = unpackedPersonFormDataStream
        .filter(
            (k, personMetadataUnpackedEvent) -> {
              Optional<EntityTag> entityTagById = entityTagService.findEntityTagById(
                  personMetadataUnpackedEvent.getMetaDataEvent().getEntityTagId());
              return entityTagById.map(
                  entityTag -> entityTag.getAggregationMethod() != null && (entityTag.getAggregationMethod().contains("count")
                      && entityTag.getValueType().equals("string"))).orElse(false);
            })
        .groupBy((k, v) -> v.getMetaDataEvent().getTagData().getMeta().getPlanId() + "_"
            + v.getEntityId() + "_" + v.getAncestorNode() + "_" + v.getMetaDataEvent().getTag());

    KTable<String, PersonFormDataAggregateEvent> stringPersonFormDataAggregate = countStream.aggregate(
        PersonFormDataAggregateEvent::new,
        (k, v, agg) -> {

          String previousValue = null;

          if (agg.getTagValue() != null) {
            previousValue = agg.getTagValue();
          }
          if (v.getMetaDataEvent().getTagData().getValue().getValueString() != null) {
            agg.setTagValue(v.getMetaDataEvent().getTagData().getValue().getValueString());
          }
          agg.setPreviousTagValue(previousValue);
          agg.setTag(v.getMetaDataEvent().getTag());
          agg.setAncestorNode(v.getAncestorNode());
          agg.setPlan(v.getMetaDataEvent().getTagData().getMeta().getPlanId());
          agg.setHierarchyIdentifier(v.getHierarchyIdentifier());
          agg.setThisLocation(v.getThisLocationId());
          agg.setEntityId(v.getEntityId());
          return agg;
        }, Materialized.<String, PersonFormDataAggregateEvent, KeyValueStore<Bytes, byte[]>>as(
                kafkaProperties.getStoreMap().get(KafkaConstants.personFormDataString))
            .withKeySerde(Serdes.String())
            .withValueSerde(new JsonSerde<>(PersonFormDataAggregateEvent.class)));

    KStream<String, PersonFormDataAggregateEvent> stringPersonFormDataAggregateEventKStream = stringPersonFormDataAggregate.toStream();
    stringPersonFormDataAggregateEventKStream
        .peek((k, v) -> streamLog.debug("stringPersonFormDataAggregate k: {} v: {}", k, v));

    KStream<String, PersonFormDataAggregateEvent> stringPersonFormDataAggregateEventKStream1 = stringPersonFormDataAggregateEventKStream.flatMapValues(
        (k, v) -> {
          List<PersonFormDataAggregateEvent> personFormDataAggregateEvents = new ArrayList<>();
          if (v.getPreviousTagValue() != null) {
            PersonFormDataAggregateEvent personFormDataAggregateEvent = new PersonFormDataAggregateEvent();
            personFormDataAggregateEvent.setPlan(v.getPlan());
            personFormDataAggregateEvent.setThisLocation(v.getThisLocation());
            personFormDataAggregateEvent.setHierarchyIdentifier(v.getHierarchyIdentifier());
            personFormDataAggregateEvent.setTag(v.getTag());
            personFormDataAggregateEvent.setEntityId(v.getEntityId());
            personFormDataAggregateEvent.setAncestorNode(v.getAncestorNode());
            personFormDataAggregateEvent.setTagValue(v.getTagValue());
            personFormDataAggregateEvent.setIncrement(-1L);
            personFormDataAggregateEvents.add(personFormDataAggregateEvent);
          }
          PersonFormDataAggregateEvent personFormDataAggregateEvent = new PersonFormDataAggregateEvent();
          personFormDataAggregateEvent.setPlan(v.getPlan());
          personFormDataAggregateEvent.setEntityId(v.getEntityId());
          personFormDataAggregateEvent.setThisLocation(v.getThisLocation());
          personFormDataAggregateEvent.setHierarchyIdentifier(v.getHierarchyIdentifier());
          personFormDataAggregateEvent.setTag(v.getTag());
          personFormDataAggregateEvent.setAncestorNode(v.getAncestorNode());
          personFormDataAggregateEvent.setTagValue(v.getTagValue());
          personFormDataAggregateEvent.setIncrement(1L);
          personFormDataAggregateEvents.add(personFormDataAggregateEvent);
          return personFormDataAggregateEvents;
        });

    stringPersonFormDataAggregateEventKStream1
        .peek((k, v) -> streamLog.debug("stringPersonFormDataAggregateEventKStream1 k: {} v: {}", k,
            v));

    KGroupedStream<String, PersonFormDataAggregateEvent> stringPersonFormDataAggregateEventKGroupedStream = stringPersonFormDataAggregateEventKStream1
        .groupBy((k, personFormDataAggregateEvent) ->
            personFormDataAggregateEvent.getPlan() + "_"
                + personFormDataAggregateEvent.getHierarchyIdentifier() + "_"
                + personFormDataAggregateEvent.getAncestorNode() + "_"
                + personFormDataAggregateEvent.getTag() + "-"
                + personFormDataAggregateEvent.getTagValue());

    KTable<String, PersonFormDataCountAggregateEvent> personStringCountAggregate = stringPersonFormDataAggregateEventKGroupedStream.aggregate(
        PersonFormDataCountAggregateEvent::new,
        (k, v, agg) ->
        {
          if (agg.getCount() != null) {
            agg.setCount(agg.getCount() + v.getIncrement());
          } else {
            agg.setCount(v.getIncrement() > 0 ? v.getIncrement() : 0L);
          }
          return agg;
        }, Materialized.<String, PersonFormDataCountAggregateEvent, KeyValueStore<Bytes, byte[]>>as(
                kafkaProperties.getStoreMap().get(KafkaConstants.personFormDataStringCount))
            .withKeySerde(Serdes.String())
            .withValueSerde(new JsonSerde<>(PersonFormDataCountAggregateEvent.class))

    );

    personStringCountAggregate.toStream()
        .peek((k, v) -> streamLog.debug("personStringCountAggregate k: {} v: {}", k, v));

    //////////////////summing and averaging

    KGroupedStream<String, PersonMetadataUnpackedEvent> personIntegerStream = unpackedPersonFormDataStream
        .filter(
            (k, personMetadataUnpackedEvent) -> {
              Optional<EntityTag> entityTagById = entityTagService.findEntityTagById(
                  personMetadataUnpackedEvent.getMetaDataEvent().getEntityTagId());
              return entityTagById.map(entityTag -> (
                  (entityTag.getAggregationMethod() != null && (entityTag.getAggregationMethod().contains("sum")
                      || entityTag.getAggregationMethod().contains("average"))
                      && entityTag.getValueType().equals("integer")))).orElse(false);
            })
        .groupBy((k, v) -> v.getMetaDataEvent().getTagData().getMeta().getPlanId() + "_"
            + v.getEntityId() + "_" + v.getAncestorNode() + "_" + v.getMetaDataEvent().getTag());

    KTable<String, PersonFormDataAggregateEvent> integerPersonFormDataAggregate = personIntegerStream.aggregate(
        PersonFormDataAggregateEvent::new,
        (k, v, agg) -> {

          Integer previousIntegerValue = null;

          if (agg.getTagIntegerValue() != null) {
            previousIntegerValue = agg.getTagIntegerValue();
          }
          if (v.getMetaDataEvent().getTagData().getValue().getValueInteger() != null) {
            agg.setTagIntegerValue(v.getMetaDataEvent().getTagData().getValue().getValueInteger());
          }

          agg.setPreviousTagIntegerValue(previousIntegerValue);
          agg.setTag(v.getMetaDataEvent().getTag());
          agg.setAncestorNode(v.getAncestorNode());
          agg.setPlan(v.getMetaDataEvent().getTagData().getMeta().getPlanId());
          agg.setHierarchyIdentifier(v.getHierarchyIdentifier());
          agg.setThisLocation(v.getThisLocationId());
          agg.setEntityId(v.getEntityId());
          return agg;
        }, Materialized.<String, PersonFormDataAggregateEvent, KeyValueStore<Bytes, byte[]>>as(
                kafkaProperties.getStoreMap().get(KafkaConstants.personFormDataInteger))
            .withKeySerde(Serdes.String())
            .withValueSerde(new JsonSerde<>(PersonFormDataAggregateEvent.class)));
//
//    KStream<String, PersonFormDataAggregateEvent> integerPersonFormDataAggregateEventKStream = integerPersonFormDataAggregate.toStream();
//    integerPersonFormDataAggregateEventKStream
//        .peek((k,v)->streamLog.debug("integerPersonFormDataAggregateEventKStream k: {} v: {}",k,v));
//
//    KStream<String, PersonFormDataAggregateEvent> integerPersonFormDataAggregateEventKStream1 = stringPersonFormDataAggregateEventKStream.flatMapValues(
//        (k, v) -> {
//          List<PersonFormDataAggregateEvent> personFormDataAggregateEvents = new ArrayList<>();
//          if (v.getPreviousTagValue() != null) {
//            PersonFormDataAggregateEvent personFormDataAggregateEvent = new PersonFormDataAggregateEvent();
//            personFormDataAggregateEvent.setPlan(v.getPlan());
//            personFormDataAggregateEvent.setThisLocation(v.getThisLocation());
//            personFormDataAggregateEvent.setHierarchyIdentifier(v.getHierarchyIdentifier());
//            personFormDataAggregateEvent.setTag(v.getTag());
//            personFormDataAggregateEvent.setEntityId(v.getEntityId());
//            personFormDataAggregateEvent.setAncestorNode(v.getAncestorNode());
//            personFormDataAggregateEvent.setIncrement(-1L * v.getPreviousTagIntegerValue());
//            personFormDataAggregateEvent.setAverageCounter(-1L);
//            personFormDataAggregateEvents.add(personFormDataAggregateEvent);
//          }
//          PersonFormDataAggregateEvent personFormDataAggregateEvent = new PersonFormDataAggregateEvent();
//          personFormDataAggregateEvent.setPlan(v.getPlan());
//          personFormDataAggregateEvent.setEntityId(v.getEntityId());
//          personFormDataAggregateEvent.setThisLocation(v.getThisLocation());
//          personFormDataAggregateEvent.setHierarchyIdentifier(v.getHierarchyIdentifier());
//          personFormDataAggregateEvent.setTag(v.getTag());
//          personFormDataAggregateEvent.setAncestorNode(v.getAncestorNode());
//          personFormDataAggregateEvent.setAverageCounter(1L);
//          personFormDataAggregateEvent.setIncrement((long) v.getTagIntegerValue());
//          personFormDataAggregateEvents.add(personFormDataAggregateEvent);
//          return personFormDataAggregateEvents;
//        });
//
//
//    integerPersonFormDataAggregateEventKStream1
//        .peek((k,v)->streamLog.debug("integerPersonFormDataAggregateEventKStream1 k: {} v: {}",k,v));
//
//    KGroupedStream<String, PersonFormDataAggregateEvent> integerPersonFormDataAggregateEventKGroupedStream = integerPersonFormDataAggregateEventKStream1
//        .groupBy((k, personFormDataAggregateEvent) ->
//            personFormDataAggregateEvent.getPlan() + "_"
//                + personFormDataAggregateEvent.getHierarchyIdentifier() + "_"
//                + personFormDataAggregateEvent.getAncestorNode()+"_"
//                + personFormDataAggregateEvent.getTag());
//
//    KTable<String, PersonFormDataSumAggregateEvent> personIntegerSumAggregate = integerPersonFormDataAggregateEventKGroupedStream.aggregate(
//        PersonFormDataSumAggregateEvent::new,
//        (k, v, agg) ->
//        {
//          if (agg.getSum() != null) {
//            agg.setSum(agg.getSum() + v.getIncrement());
//          } else {
//            agg.setSum(v.getIncrement() > 0 ? v.getIncrement() : 0L);
//          }
//
//          if (agg.getCounter() != null) {
//            agg.setCounter(agg.getCounter() + v.getAverageCounter());
//          } else {
//            agg.setCounter(v.getAverageCounter() > 0 ? v.getAverageCounter() : 0L);
//          }
//
//          agg.setAverage((double) agg.getSum() / (double) agg.getCounter());
//
//          return agg;
//        }, Materialized.<String, PersonFormDataSumAggregateEvent, KeyValueStore<Bytes, byte[]>>as(
//                kafkaProperties.getStoreMap().get(KafkaConstants.personFormDataIntegerSumOrAverage))
//            .withKeySerde(Serdes.String())
//            .withValueSerde(new JsonSerde<>(PersonFormDataSumAggregateEvent.class))
//
//    );
//
//    personIntegerSumAggregate.toStream()
//        .peek((k,v)->streamLog.debug("personIntegerSumAggregate k: {} v: {}",k,v));

    return personFormDataStream;
  }

  private String getPlanAncestorHierarchyKey(PersonMetadataUnpackedEvent personMetadata) {
    return personMetadata.getMetaDataEvent().getTagData().getMeta().getPlanId() + "_" +
        personMetadata.getAncestorNode() + "_" +
        personMetadata.getHierarchyIdentifier();
  }

  private PersonMetadataEvent getPersonMetadataUnpackedForHierarchy(
      PersonMetadataEvent personMetadata) {

    LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(
        personMetadata.getHierarchyIdentifier());

    return getPersonMetadataEventWithLocationHierarchy(personMetadata,
        locationHierarchy);
  }

  private List<PersonMetadataEvent> getUnpackedPersonMetadataPerAncestorList(
      PersonMetadataEvent personMetadata) {
    return personMetadata.getLocationIdList().stream().flatMap(
            locationId -> getPersonMetadataEventStreamPerAncestor(personMetadata, locationId))
        .collect(Collectors.toList());
  }

  private Stream<PersonMetadataEvent> getPersonMetadataEventStreamPerAncestor(
      PersonMetadataEvent personMetadata, UUID locationId) {
    LocationRelationship locationRelationShip = locationRelationshipService.getLocationRelationshipsForLocation(
        personMetadata.getHierarchyIdentifier(), locationId);
    return locationRelationShip.getAncestry()
        .stream()
        .map(ancestorNode -> getPersonMetadataEventWithAncestorNode(personMetadata, locationId,
            ancestorNode));
  }

  private PersonMetadataEvent getPersonMetadataEventWithAncestorNode(
      PersonMetadataEvent personMetadata,
      UUID locationId, UUID ancestorNode) {
    PersonMetadataEvent personMetadataEvent = new PersonMetadataEvent();
    personMetadataEvent.setHierarchyIdentifier(
        personMetadata.getHierarchyIdentifier());
    personMetadataEvent.setMetaDataEvents(personMetadata.getMetaDataEvents());
    personMetadataEvent.setEntityId(personMetadata.getEntityId());
    personMetadataEvent.setThisLocation(locationId);
    personMetadataEvent.setAncestorNode(ancestorNode);
    personMetadataEvent.setPlanIdentifier(personMetadata.getPlanIdentifier());
    return personMetadataEvent;
  }

  private PersonMetadataEvent getPersonMetadataEventWithLocationHierarchy(
      PersonMetadataEvent personMetadata,
      LocationHierarchy locationHierarchy) {
    PersonMetadataEvent personMetadataEvent = new PersonMetadataEvent();
    personMetadataEvent.setHierarchyIdentifier(locationHierarchy.getIdentifier());
    personMetadataEvent.setMetaDataEvents(personMetadata.getMetaDataEvents());
    personMetadataEvent.setEntityId(personMetadata.getEntityId());
    personMetadataEvent.setLocationIdList(personMetadata.getLocationIdList());
    personMetadataEvent.setPlanIdentifier(personMetadata.getPlanIdentifier());
    return personMetadataEvent;
  }

  private List<PersonMetadataUnpackedEvent> getUnpackedPersonMetadataPerMetaItemList(
      PersonMetadataEvent personMetadata) {
    return personMetadata.getMetaDataEvents()
        .stream().map(
            metaDataEvent -> getPersonMetadataUnpackedEventWithIndividualMetadataItem(
                personMetadata, metaDataEvent)).collect(Collectors.toList());
  }

  private PersonMetadataUnpackedEvent getPersonMetadataUnpackedEventWithIndividualMetadataItem(
      PersonMetadataEvent personMetadata, MetaDataEvent metaDataEvent) {
    PersonMetadataUnpackedEvent locationMetadataUnpackedEvent = new PersonMetadataUnpackedEvent();
    locationMetadataUnpackedEvent.setHierarchyIdentifier(
        personMetadata.getHierarchyIdentifier());
    locationMetadataUnpackedEvent.setEntityId(personMetadata.getEntityId());
    locationMetadataUnpackedEvent.setAncestorNode(personMetadata.getAncestorNode());
    locationMetadataUnpackedEvent.setMetaDataEvent(metaDataEvent);
    locationMetadataUnpackedEvent.setThisLocationId(personMetadata.getThisLocation());
    return locationMetadataUnpackedEvent;
  }


  @Bean
  KStream<UUID, LocationMetadataEvent> locationFormDataCountsAggregator(
      StreamsBuilder streamsBuilder) {

    KStream<UUID, LocationMetadataEvent> locationFormDataStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(KafkaConstants.LOCATION_METADATA_UPDATE),
        Consumed.with(Serdes.UUID(), new JsonSerde<>(LocationMetadataEvent.class)));

    locationFormDataStream.peek(
        (k, v) -> streamLog.debug("locationFormDataStream - k: {} v: {}", k, v));

    KStream<UUID, LocationMetadataUnpackedEvent> locationFormDataStreamUnpackedEventKStream = locationFormDataStream
        .flatMapValues(
            (k, locationMetadata) -> getPersonFormDataUnpackedByMetadataItems(locationMetadata));

    KStream<String, LocationMetadataUnpackedEvent> unpackedLocationFormDataStream = locationFormDataStreamUnpackedEventKStream
        .flatMapValues(
            (k, locationMetadataUnpacked) -> getLocationMetadataUnpackedByAncestry(
                locationMetadataUnpacked))
        .selectKey((k, locationMetadata) -> getPlanAncestorHierarchyEntityKey(locationMetadata));

    unpackedLocationFormDataStream.peek(
        (k, v) -> streamLog.debug("unpackedLocationFormDataStream - k: {} v: {}", k, v));

    //////////////////summing and averaging

    KGroupedStream<String, LocationMetadataUnpackedEvent> locationSumStreamInteger = unpackedLocationFormDataStream
        .filter(
            (k, locationFormDataUnpackedEvent) -> {
              Optional<EntityTag> entityTagById = entityTagService.findEntityTagById(
                  locationFormDataUnpackedEvent.getMetaDataEvent().getEntityTagId());
              return entityTagById.map(entityTag -> (
                  (entityTag.getAggregationMethod() != null && (entityTag.getAggregationMethod().contains("sum")
                      || entityTag.getAggregationMethod().contains("average"))
                      && entityTag.getValueType().equals("integer")))).orElse(false);
            })
        .groupBy((k, v) -> v.getMetaDataEvent().getTagData().getMeta().getPlanId() + "_"
            + v.getEntityId() + "_" + v.getAncestorNode() + "_" + v.getMetaDataEvent().getTag() +

            (v.getMetaDataEvent().isDateScope()?"_"+v.getMetaDataEvent().getDateForDateScope():""));

    KTable<String, LocationFormDataAggregateEvent> integerLocationFormDataAggregate = locationSumStreamInteger.aggregate(
        LocationFormDataAggregateEvent::new,
        (k, v, agg) -> {

          Integer previousIntegerValue = null;

          if (v.getMetaDataEvent().getTagData().getValue().getValueInteger() !=null) {
            if (agg.getTagIntegerValue() != null && (!Objects.equals(agg.getTagIntegerValue(),
                v.getMetaDataEvent().getTagData().getValue().getValueInteger()))) {

              if (agg.getTagIntegerValue() != null) {
                previousIntegerValue = agg.getTagIntegerValue();
              }

              agg.setPreviousTagIntegerValue(previousIntegerValue);
            }

            if (v.getMetaDataEvent().getTagData().getValue().getValueInteger() != null) {
              agg.setTagIntegerValue(
                  v.getMetaDataEvent().getTagData().getValue().getValueInteger());
            }
            agg.setTag(v.getMetaDataEvent().getTag());
            agg.setAncestorNode(v.getAncestorNode());
            agg.setPlan(v.getMetaDataEvent().getTagData().getMeta().getPlanId());
            agg.setHierarchyIdentifier(v.getHierarchyIdentifier());
            agg.setEntityId(v.getEntityId());
          }
          return agg;
        }, Materialized.<String, LocationFormDataAggregateEvent, KeyValueStore<Bytes, byte[]>>as(
                kafkaProperties.getStoreMap().get(KafkaConstants.locationFormDataInteger))
            .withKeySerde(Serdes.String())
            .withValueSerde(new JsonSerde<>(LocationFormDataAggregateEvent.class)));

    KStream<String, LocationFormDataAggregateEvent> stringLocationFormDataAggregateEventKStream = integerLocationFormDataAggregate.toStream();
    stringLocationFormDataAggregateEventKStream
        .peek(
            (k, v) -> streamLog.debug("stringLocationFormDataAggregateEventKStream k: {} v: {}", k,
                v));

    KStream<String, LocationFormDataAggregateEvent> integerLocationFormDataAggregateEventKStream1 = stringLocationFormDataAggregateEventKStream.flatMapValues(
        (k, v) -> {
          List<LocationFormDataAggregateEvent> locationFormDataAggregateEvents = new ArrayList<>();
          if (v.getPreviousTagIntegerValue() != null) {
            LocationFormDataAggregateEvent locationFormDataAggregateEvent = new LocationFormDataAggregateEvent();
            locationFormDataAggregateEvent.setPlan(v.getPlan());
            locationFormDataAggregateEvent.setHierarchyIdentifier(v.getHierarchyIdentifier());
            locationFormDataAggregateEvent.setTag(v.getTag());
            locationFormDataAggregateEvent.setEntityId(v.getEntityId());
            locationFormDataAggregateEvent.setAncestorNode(v.getAncestorNode());
            locationFormDataAggregateEvent.setIncrement(-1L * v.getPreviousTagIntegerValue());
            locationFormDataAggregateEvent.setAverageCounter(-1L);
            locationFormDataAggregateEvents.add(locationFormDataAggregateEvent);
          }
          LocationFormDataAggregateEvent locationFormDataAggregateEvent = new LocationFormDataAggregateEvent();
          locationFormDataAggregateEvent.setPlan(v.getPlan());
          locationFormDataAggregateEvent.setEntityId(v.getEntityId());
          locationFormDataAggregateEvent.setHierarchyIdentifier(v.getHierarchyIdentifier());
          locationFormDataAggregateEvent.setTag(v.getTag());
          locationFormDataAggregateEvent.setAncestorNode(v.getAncestorNode());
          locationFormDataAggregateEvent.setTagValue(v.getTagValue());
          locationFormDataAggregateEvent.setAverageCounter(1L);
          locationFormDataAggregateEvent.setIncrement((long) v.getTagIntegerValue());
          locationFormDataAggregateEvents.add(locationFormDataAggregateEvent);
          return locationFormDataAggregateEvents;
        });

    integerLocationFormDataAggregateEventKStream1
        .peek(
            (k, v) -> streamLog.debug("stringLocationFormDataAggregateEventKStream1 k: {} v: {}", k,
                v));

    KGroupedStream<String, LocationFormDataAggregateEvent> stringLocationFormDataAggregateEventKGroupedStream = integerLocationFormDataAggregateEventKStream1
        .groupBy((k, locationFormDataAggregateEvent) ->
            locationFormDataAggregateEvent.getPlan() + "_"
                + locationFormDataAggregateEvent.getHierarchyIdentifier() + "_"
                + locationFormDataAggregateEvent.getAncestorNode() + "_"
                + locationFormDataAggregateEvent.getTag());

    KTable<String, LocationFormDataSumAggregateEvent> locationIntegerSumAggregate = stringLocationFormDataAggregateEventKGroupedStream.aggregate(
        LocationFormDataSumAggregateEvent::new,
        (k, v, agg) ->
        {
          if (agg.getSum() != null) {
            agg.setSum(agg.getSum() + v.getIncrement());
          } else {
            agg.setSum(v.getIncrement() > 0 ? v.getIncrement() : 0L);
          }

          if (agg.getCounter() != null) {
            agg.setCounter(agg.getCounter() + v.getAverageCounter());
          } else {
            agg.setCounter(v.getAverageCounter() > 0 ? v.getAverageCounter() : 0L);
          }

          agg.setAverage((double) agg.getSum() / (double) agg.getCounter());

          return agg;
        }, Materialized.<String, LocationFormDataSumAggregateEvent, KeyValueStore<Bytes, byte[]>>as(
                kafkaProperties.getStoreMap().get(KafkaConstants.locationFormDataIntegerSumOrAverage))
            .withKeySerde(Serdes.String())
            .withValueSerde(new JsonSerde<>(LocationFormDataSumAggregateEvent.class))

    );

    locationIntegerSumAggregate.toStream()
        .peek((k, v) -> streamLog.debug("locationStringCountAggregate k: {} v: {}", k, v));

    //////////////counting
    KGroupedStream<String, LocationMetadataUnpackedEvent> countStream = unpackedLocationFormDataStream
        .filter(
            (k, locationMetadataUnpackedEvent) -> {
              Optional<EntityTag> entityTagById = entityTagService.findEntityTagById(
                  locationMetadataUnpackedEvent.getMetaDataEvent().getEntityTagId());
              return entityTagById.map(
                  entityTag -> entityTag.getAggregationMethod() != null && (entityTag.getAggregationMethod().contains("count")
                      && entityTag.getValueType().equals("string"))).orElse(false);
            })
        .groupBy((k, v) -> v.getMetaDataEvent().getTagData().getMeta().getPlanId() + "_"
            + v.getEntityId() + "_" + v.getAncestorNode() + "_" + v.getMetaDataEvent().getTag());

    KTable<String, LocationFormDataAggregateEvent> stringLocationFormDataAggregate = countStream.aggregate(
        LocationFormDataAggregateEvent::new,
        (k, v, agg) -> {

          String previousValue = null;

          if (agg.getTagValue() != null) {
            previousValue = agg.getTagValue();
          }
          if (v.getMetaDataEvent().getTagData().getValue().getValueString() != null) {
            agg.setTagValue(v.getMetaDataEvent().getTagData().getValue().getValueString());
          }
          agg.setPreviousTagValue(previousValue);
          agg.setTag(v.getMetaDataEvent().getTag());
          agg.setAncestorNode(v.getAncestorNode());
          agg.setPlan(v.getMetaDataEvent().getTagData().getMeta().getPlanId());
          agg.setHierarchyIdentifier(v.getHierarchyIdentifier());
          agg.setEntityId(v.getEntityId());
          return agg;
        }, Materialized.<String, LocationFormDataAggregateEvent, KeyValueStore<Bytes, byte[]>>as(
                kafkaProperties.getStoreMap().get(KafkaConstants.locationFormDataString))
            .withKeySerde(Serdes.String())
            .withValueSerde(new JsonSerde<>(LocationFormDataAggregateEvent.class)));

    KStream<String, LocationFormDataAggregateEvent> stringCountLocationFormDataAggregateEventKStream = stringLocationFormDataAggregate.toStream();
    stringCountLocationFormDataAggregateEventKStream
        .peek((k, v) -> streamLog.debug(
            "stringCountLocationFormDataAggregateEventKStream k: {} v: {}", k, v));

    KStream<String, LocationFormDataAggregateEvent> stringLocationFormDataAggregateEventKStream1 = stringCountLocationFormDataAggregateEventKStream.flatMapValues(
        (k, v) -> {
          List<LocationFormDataAggregateEvent> locationFormDataAggregateEvents = new ArrayList<>();
          if (v.getPreviousTagValue() != null) {
            LocationFormDataAggregateEvent locationFormDataAggregateEvent = new LocationFormDataAggregateEvent();
            locationFormDataAggregateEvent.setPlan(v.getPlan());
            locationFormDataAggregateEvent.setHierarchyIdentifier(v.getHierarchyIdentifier());
            locationFormDataAggregateEvent.setTag(v.getTag());
            locationFormDataAggregateEvent.setEntityId(v.getEntityId());
            locationFormDataAggregateEvent.setAncestorNode(v.getAncestorNode());
            locationFormDataAggregateEvent.setTagValue(v.getTagValue());
            locationFormDataAggregateEvent.setIncrement(-1L);
            locationFormDataAggregateEvents.add(locationFormDataAggregateEvent);
          }
          LocationFormDataAggregateEvent locationFormDataAggregateEvent = new LocationFormDataAggregateEvent();
          locationFormDataAggregateEvent.setPlan(v.getPlan());
          locationFormDataAggregateEvent.setEntityId(v.getEntityId());
          locationFormDataAggregateEvent.setHierarchyIdentifier(v.getHierarchyIdentifier());
          locationFormDataAggregateEvent.setTag(v.getTag());
          locationFormDataAggregateEvent.setAncestorNode(v.getAncestorNode());
          locationFormDataAggregateEvent.setTagValue(v.getTagValue());
          locationFormDataAggregateEvent.setIncrement(1L);
          locationFormDataAggregateEvents.add(locationFormDataAggregateEvent);
          return locationFormDataAggregateEvents;
        });

    stringLocationFormDataAggregateEventKStream1
        .peek(
            (k, v) -> streamLog.debug("stringLocationFormDataAggregateEventKStream1 k: {} v: {}", k,
                v));

    KGroupedStream<String, LocationFormDataAggregateEvent> stringLocationCountFormDataAggregateEventKGroupedStream = stringLocationFormDataAggregateEventKStream1
        .groupBy((k, personFormDataAggregateEvent) ->
            personFormDataAggregateEvent.getPlan() + "_"
                + personFormDataAggregateEvent.getHierarchyIdentifier() + "_"
                + personFormDataAggregateEvent.getAncestorNode() + "_"
                + personFormDataAggregateEvent.getTag() + "-"
                + personFormDataAggregateEvent.getTagValue());

    KTable<String, LocationFormDataCountAggregateEvent> locationStringCountAggregate = stringLocationCountFormDataAggregateEventKGroupedStream.aggregate(
        LocationFormDataCountAggregateEvent::new,
        (k, v, agg) ->
        {
          if (agg.getCount() != null) {
            agg.setCount(agg.getCount() + v.getIncrement());
          } else {
            agg.setCount(v.getIncrement() > 0 ? v.getIncrement() : 0L);
          }
          return agg;
        },
        Materialized.<String, LocationFormDataCountAggregateEvent, KeyValueStore<Bytes, byte[]>>as(
                kafkaProperties.getStoreMap().get(KafkaConstants.locationFormDataStringCount))
            .withKeySerde(Serdes.String())
            .withValueSerde(new JsonSerde<>(LocationFormDataCountAggregateEvent.class))

    );

    locationStringCountAggregate.toStream()
        .peek((k, v) -> streamLog.debug("locationStringCountAggregate k: {} v: {}", k, v));

    return locationFormDataStream;
  }


  private String getPlanAncestorHierarchyEntityKey(LocationMetadataUnpackedEvent v) {
    return v.getMetaDataEvent().getTagData().getMeta().getPlanId() + "_" +
        v.getAncestorNode() + "_" +
        v.getHierarchyIdentifier() + "_" +
        v.getEntityId();
  }

  private List<LocationMetadataUnpackedEvent> getPersonFormDataUnpackedByMetadataItems(
      LocationMetadataEvent locationMetadata) {
    return locationMetadata.getMetaDataEvents()
        .stream()
        .filter(metaDataEvent -> metaDataEvent.getType()
            .equals("FormData"))
        .map(metaDataEvent -> LocationMetadataUnpackedEvent.builder()
            .hierarchyIdentifier(locationMetadata.getHierarchyIdentifier())
            .entityId(locationMetadata.getEntityId())

            .metaDataEvent(metaDataEvent)
            .plan(metaDataEvent.getTagData().getMeta().getPlanId())
            .entityGeoLevel(locationMetadata.getEntityGeographicLevel())
            .planTargetType(locationMetadata.getPlanTargetType())
            .build())
        .collect(Collectors.toList());
  }

  private List<LocationMetadataUnpackedEvent> getLocationMetadataUnpackedByAncestry(
      LocationMetadataUnpackedEvent locationMetadata) {
    LocationRelationship locationRelationShip = locationRelationshipService.getLocationRelationshipsForLocation(
        locationMetadata.getHierarchyIdentifier(), locationMetadata.getEntityId());
    List<UUID> ancestry = locationRelationShip.getAncestry();
    ancestry.add(locationMetadata.getEntityId());
    return ancestry
        .stream()
        .map(ancestorNode -> getLocationMetadataEventPerAncestor(locationMetadata, ancestorNode))
        .collect(Collectors.toList());
  }

  private LocationMetadataUnpackedEvent getLocationMetadataEventPerAncestor(
      LocationMetadataUnpackedEvent locationMetadata,
      UUID ancestorNode) {

    return LocationMetadataUnpackedEvent.builder()
        .hierarchyIdentifier(locationMetadata.getHierarchyIdentifier())
        .metaDataEvent(locationMetadata.getMetaDataEvent())
        .entityGeoLevel(locationMetadata.getEntityGeoLevel())
        .plan(locationMetadata.getPlan())
        .identifier(locationMetadata.getIdentifier())
        .entityId(locationMetadata.getEntityId())
        .ancestorNode(ancestorNode)
        .planTargetType(locationMetadata.getPlanTargetType())
        .build();
  }

}
