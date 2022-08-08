package com.revealprecision.revealserver.messaging.streams;

import static com.revealprecision.revealserver.constants.KafkaConstants.FORM_EVENT_CONSUMPTION;
import static com.revealprecision.revealserver.constants.KafkaConstants.FORM_OBSERVATIONS;

import com.revealprecision.revealserver.api.v1.dto.factory.LocationFormDataAggregateEventFactory;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.FormDataEntityTagValueEvent;
import com.revealprecision.revealserver.messaging.message.FormObservationsEvent;
import com.revealprecision.revealserver.messaging.message.LocationFormDataAggregateEvent;
import com.revealprecision.revealserver.messaging.message.LocationFormDataSumAggregateEvent;
import com.revealprecision.revealserver.messaging.message.PersonMetadataEvent;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
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
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
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
  private final Logger formDataLog = LoggerFactory.getLogger("form-data-file");

  @Bean
  KStream<UUID, PersonMetadataEvent> personFormDataProcessor(StreamsBuilder streamsBuilder) {
//TODO: Bring this back after mdalite dashboard is confirmed

    return null;
  }

  @Bean
  KStream<UUID, FormDataEntityTagValueEvent> locationFormDataCountsAggregator(
      StreamsBuilder streamsBuilder) {

    KStream<UUID, FormDataEntityTagValueEvent> locationFormDataStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(FORM_EVENT_CONSUMPTION),
        Consumed.with(Serdes.UUID(), new JsonSerde<>(FormDataEntityTagValueEvent.class)));

    locationFormDataStream.peek(
        (k, v) -> formDataLog.debug("locationFormDataStream - k: {} v: {}", k, v));

    KStream<String, FormDataEntityTagValueEvent> unpackedLocationFormDataStream = locationFormDataStream
        .flatMapValues((k, formDataEntityTagValueEvent) -> getMetadataObjEventUnpackedByAncestry(
            formDataEntityTagValueEvent))
        .selectKey((k, formDataEntityTagValueEvent) -> getPlanAncestorHierarchyEntityKey(
            formDataEntityTagValueEvent));

    unpackedLocationFormDataStream.peek(
        (k, v) -> formDataLog.debug("unpackedLocationFormDataStream - k: {} v: {}", k, v));

    //////////////////summing and averaging

    KGroupedStream<String, FormDataEntityTagValueEvent> locationSumStreamInteger = unpackedLocationFormDataStream
        .filter(
            (k, formDataEntityTagValueEvent) ->
                (formDataEntityTagValueEvent.getEntityTagEvent().getAggregationMethod() != null && (
                    formDataEntityTagValueEvent.getEntityTagEvent().getAggregationMethod()
                        .contains("sum")
                        || formDataEntityTagValueEvent.getEntityTagEvent().getAggregationMethod()
                        .contains("average"))
                    && formDataEntityTagValueEvent.getEntityTagEvent().getValueType()
                    .equals("integer"))
        )
        .groupBy((k, v) -> v.getPlanIdentifier() + "_"
            + v.getEntityIdentifier() + "_" + v.getAncestor() + "_" + v.getEntityTagEvent().getTag()
            +
            (v.getSupervisor() != null ? "_" + v.getSupervisor() : "") + (v.getCddName() != null ?
            "_" + v.getCddName() : "") +
            (v.getEntityTagEvent().getScope().equals("Date") ? "_" + v.getDateForScopeDate() : ""));

    KTable<String, LocationFormDataAggregateEvent> integerLocationFormDataAggregate = locationSumStreamInteger.aggregate(
        LocationFormDataAggregateEvent::new,
        (k, v, agg) -> {

          formDataLog.trace("incoming eventId {} current eventId {}", v.getEventId(),
              agg.getEventId());
          Integer previousIntegerValue = null;
          if (v.getValue() != null) {

            if (agg.getTagIntegerValue() != null && (!Objects.equals(agg.getTagIntegerValue(),
                v.getValue()))) {

              if (agg.getTagIntegerValue() != null) {
                previousIntegerValue = agg.getTagIntegerValue();
              }

              agg.setPreviousTagIntegerValue(previousIntegerValue);
            }

            if (v.getValue() != null) {
              agg.setTagIntegerValue(
                  (Integer) v.getValue());
            }
            agg.setTagId(v.getEntityTagEvent().getIdentifier());
            agg.setTag(v.getEntityTagEvent().getTag());
            agg.setAncestorNode(v.getAncestor());
            agg.setPlan(v.getPlanIdentifier());
            agg.setHierarchyIdentifier(v.getLocationHierarchyIdentifier());
            agg.setEntityId(v.getEntityIdentifier());
            agg.setCddName(v.getCddName());
            agg.setSupervisor(v.getSupervisor());
          }
          return agg;
        }, Materialized.<String, LocationFormDataAggregateEvent, KeyValueStore<Bytes, byte[]>>as(
                kafkaProperties.getStoreMap().get(KafkaConstants.locationFormDataInteger))
            .withKeySerde(Serdes.String())
            .withValueSerde(new JsonSerde<>(LocationFormDataAggregateEvent.class)));

    KStream<String, LocationFormDataAggregateEvent> stringLocationFormDataAggregateEventKStream = integerLocationFormDataAggregate.toStream();

    stringLocationFormDataAggregateEventKStream
        .peek((k, v) -> formDataLog.debug("stringLocationFormDataAggregateEventKStream k: {} v: {}",
            k, v));

    KStream<String, LocationFormDataAggregateEvent> integerLocationFormDataAggregateEventKStream1 = stringLocationFormDataAggregateEventKStream
        .flatMapValues((k, v) -> getLocationFormDataAggregateEvents(v));

    integerLocationFormDataAggregateEventKStream1
        .peek(
            (k, v) -> formDataLog.debug("stringLocationFormDataAggregateEventKStream1 k: {} v: {}",
                k, v));

    KGroupedStream<String, LocationFormDataAggregateEvent> stringLocationFormDataAggregateEventKGroupedStream = integerLocationFormDataAggregateEventKStream1
        .groupBy((k, locationFormDataAggregateEvent) ->
            locationFormDataAggregateEvent.getPlan() + "_"
                + locationFormDataAggregateEvent.getHierarchyIdentifier() + "_"
                + locationFormDataAggregateEvent.getAncestorNode() + "_"
                + locationFormDataAggregateEvent.getTag());

    KTable<String, LocationFormDataSumAggregateEvent> locationIntegerSumAggregate = stringLocationFormDataAggregateEventKGroupedStream.aggregate(
        LocationFormDataSumAggregateEvent::new,
        (k, v, agg) ->
            getLocationFormDataSumAggregateEvent(v, agg),
        Materialized.<String, LocationFormDataSumAggregateEvent, KeyValueStore<Bytes, byte[]>>as(
                kafkaProperties.getStoreMap().get(KafkaConstants.locationFormDataIntegerSumOrAverage))
            .withKeySerde(Serdes.String())
            .withValueSerde(new JsonSerde<>(LocationFormDataSumAggregateEvent.class)));

    KStream<String, LocationFormDataSumAggregateEvent> stringLocationFormDataSumAggregateEventKStream = locationIntegerSumAggregate.toStream();

    stringLocationFormDataSumAggregateEventKStream
        .peek((k, v) -> formDataLog.debug(
            "stringLocationFormDataSumAggregateEventKStream k: {} v: {}", k, v));
    stringLocationFormDataSumAggregateEventKStream.to(
        kafkaProperties.getTopicMap().get(KafkaConstants.METADATA_AGGREGATE),
        Produced.with(Serdes.String(), new JsonSerde<>(LocationFormDataSumAggregateEvent.class)));

    KGroupedStream<String, LocationFormDataAggregateEvent> supervisorLocationFormDataAggregateEventKGroupedStream = integerLocationFormDataAggregateEventKStream1
        .filter((k, v) -> v.getSupervisor() != null)
        .groupBy((k, locationFormDataAggregateEvent) ->
            locationFormDataAggregateEvent.getPlan() + "_"
                + locationFormDataAggregateEvent.getHierarchyIdentifier() + "_"
                + locationFormDataAggregateEvent.getAncestorNode() + "_"
                + locationFormDataAggregateEvent.getTag() + "_"
                + locationFormDataAggregateEvent.getSupervisor());

    KTable<String, LocationFormDataSumAggregateEvent> supervisorLocationIntegerSumAggregate = supervisorLocationFormDataAggregateEventKGroupedStream.aggregate(
        LocationFormDataSumAggregateEvent::new,
        (k, v, agg) ->
            getLocationFormDataSumAggregateEvent(v, agg),
        Materialized.<String, LocationFormDataSumAggregateEvent, KeyValueStore<Bytes, byte[]>>as(
                kafkaProperties.getStoreMap()
                    .get(KafkaConstants.supervisorLocationFormDataIntegerSumOrAverage))
            .withKeySerde(Serdes.String())
            .withValueSerde(new JsonSerde<>(LocationFormDataSumAggregateEvent.class)));

    supervisorLocationIntegerSumAggregate.toStream()
        .peek(
            (k, v) -> formDataLog.debug("supervisorLocationIntegerSumAggregate k: {} v: {}", k, v));

    KGroupedStream<String, LocationFormDataAggregateEvent> cddLocationFormDataAggregateEventKGroupedStream = integerLocationFormDataAggregateEventKStream1
        .filter((k, v) -> v.getSupervisor() != null && v.getCddName() != null)
        .groupBy((k, locationFormDataAggregateEvent) ->
            locationFormDataAggregateEvent.getPlan() + "_"
                + locationFormDataAggregateEvent.getHierarchyIdentifier() + "_"
                + locationFormDataAggregateEvent.getAncestorNode() + "_"
                + locationFormDataAggregateEvent.getTag() + "_"
                + locationFormDataAggregateEvent.getSupervisor() + "_"
                + locationFormDataAggregateEvent.getCddName());

    KTable<String, LocationFormDataSumAggregateEvent> cddLocationIntegerSumAggregate = cddLocationFormDataAggregateEventKGroupedStream.aggregate(
        LocationFormDataSumAggregateEvent::new,
        (k, v, agg) ->
            getLocationFormDataSumAggregateEvent(v, agg),
        Materialized.<String, LocationFormDataSumAggregateEvent, KeyValueStore<Bytes, byte[]>>as(
                kafkaProperties.getStoreMap()
                    .get(KafkaConstants.cddSupervisorLocationFormDataIntegerSumOrAverage))
            .withKeySerde(Serdes.String())
            .withValueSerde(new JsonSerde<>(LocationFormDataSumAggregateEvent.class)));

    cddLocationIntegerSumAggregate.toStream()
        .peek((k, v) -> formDataLog.debug("cddLocationIntegerSumAggregate k: {} v: {}", k, v));

    return locationFormDataStream;
  }

  @Bean
  public KStream<String, FormObservationsEvent> processFormObservations(
      StreamsBuilder streamsBuilder) {
    //Depending on final topic strategy, this can be published and consumed to a different. but for now it works here
    KStream<String, FormObservationsEvent> formObservationsEventKStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(FORM_OBSERVATIONS),
        Consumed.with(Serdes.String(), new JsonSerde<>(FormObservationsEvent.class)));

    KTable<String, FormObservationsEvent> formDataObservationPerPlanStructure = formObservationsEventKStream.toTable(
        Materialized.<String, FormObservationsEvent, KeyValueStore<Bytes, byte[]>>as(
                kafkaProperties.getStoreMap().get(KafkaConstants.formObservations))
            .withKeySerde(Serdes.String())
            .withValueSerde(new JsonSerde<>(FormObservationsEvent.class)));
    formDataObservationPerPlanStructure.toStream()
        .peek((k, v) -> formDataLog.debug("Form observations: (k,v) -> %s,%s", k, v));
    return formObservationsEventKStream;
  }

  private LocationFormDataSumAggregateEvent getLocationFormDataSumAggregateEvent(
      LocationFormDataAggregateEvent v, LocationFormDataSumAggregateEvent agg) {
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
    agg.setEntityTagIdentifier(v.getTagId());
    return agg;
  }

  private List<LocationFormDataAggregateEvent> getLocationFormDataAggregateEvents(
      LocationFormDataAggregateEvent v) {
    List<LocationFormDataAggregateEvent> locationFormDataAggregateEvents = new ArrayList<>();

    if (v.getPreviousTagIntegerValue() != null) {
      locationFormDataAggregateEvents.add(
          LocationFormDataAggregateEventFactory.getEntity(v, -1L * v.getPreviousTagIntegerValue(),
              -1L, null));
    }
    locationFormDataAggregateEvents.add(
        LocationFormDataAggregateEventFactory.getEntity(v, (long) v.getTagIntegerValue(), 1L,
            v.getTagValue()));

    return locationFormDataAggregateEvents;
  }


  private String getPlanAncestorHierarchyEntityKey(FormDataEntityTagValueEvent v) {
    return v.getPlanIdentifier() + "_" +
        v.getAncestor() + "_" +
        v.getLocationHierarchyIdentifier() + "_" +
        v.getEntityIdentifier();
  }

  private List<FormDataEntityTagValueEvent> getMetadataObjEventUnpackedByAncestry(
      FormDataEntityTagValueEvent formDataEntityTagValueEvent) {
    LocationRelationship locationRelationShip = locationRelationshipService.getLocationRelationshipsForLocation(
        formDataEntityTagValueEvent.getLocationHierarchyIdentifier(),
        formDataEntityTagValueEvent.getEntityIdentifier());
    if (locationRelationShip != null) {
      List<UUID> ancestry = locationRelationShip.getAncestry();
      ancestry.add(formDataEntityTagValueEvent.getEntityIdentifier());
      return ancestry
          .stream()
          .map(ancestorNode -> getMetadataObjEventPerAncestor(formDataEntityTagValueEvent,
              ancestorNode))
          .collect(Collectors.toList());
    }
    return null;
  }

  private FormDataEntityTagValueEvent getMetadataObjEventPerAncestor(
      FormDataEntityTagValueEvent formDataEntityTagValueEvent,
      UUID ancestorNode) {

    return FormDataEntityTagValueEvent.builder()
        .locationHierarchyIdentifier(formDataEntityTagValueEvent.getLocationHierarchyIdentifier())
        .entityTagEvent(formDataEntityTagValueEvent.getEntityTagEvent())
        .entityIdentifier(formDataEntityTagValueEvent.getEntityIdentifier())
        .planIdentifier(formDataEntityTagValueEvent.getPlanIdentifier())
        .geographicLevelName(formDataEntityTagValueEvent.getGeographicLevelName())
        .value(formDataEntityTagValueEvent.getValue())
        .ancestor(ancestorNode)
        .dateForScopeDate(formDataEntityTagValueEvent.getDateForScopeDate())
        .supervisor(formDataEntityTagValueEvent.getSupervisor())
        .cddName(formDataEntityTagValueEvent.getCddName())
        .eventId(formDataEntityTagValueEvent.getEventId())
        .build();
  }

}
