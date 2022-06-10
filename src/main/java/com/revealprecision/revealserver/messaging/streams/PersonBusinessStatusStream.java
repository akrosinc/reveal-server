package com.revealprecision.revealserver.messaging.streams;

import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.LocationPersonBusinessStateAggregate;
import com.revealprecision.revealserver.messaging.message.LocationPersonBusinessStateAggregate.PersonBusinessStatus;
import com.revealprecision.revealserver.messaging.message.LocationPersonBusinessStateCountAggregate;
import com.revealprecision.revealserver.messaging.message.LocationPersonBusinessStateStreamTransportEvent;
import com.revealprecision.revealserver.messaging.message.MetaDataEvent;
import com.revealprecision.revealserver.messaging.message.PersonBusinessStatusAggregate;
import com.revealprecision.revealserver.messaging.message.PersonBusinessStatusAggregate.CurrentState;
import com.revealprecision.revealserver.messaging.message.PersonMetadataEvent;
import com.revealprecision.revealserver.messaging.message.PersonMetadataUnpackedEvent;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.LocationHierarchyService;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
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
public class PersonBusinessStatusStream {


  private final KafkaProperties kafkaProperties;
  private final LocationRelationshipService locationRelationshipService;
  private final LocationHierarchyService locationHierarchyService;
  private final Logger streamLog = LoggerFactory.getLogger("stream-file");

  @Bean
  KStream<UUID, PersonMetadataEvent> personMetadataProcessor(StreamsBuilder streamsBuilder) {

    KStream<UUID, PersonMetadataEvent> personMetadataStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(KafkaConstants.PERSON_METADATA_UPDATE),
        Consumed.with(Serdes.UUID(), new JsonSerde<>(PersonMetadataEvent.class)));

    KStream<String, PersonMetadataUnpackedEvent> unpackedPersonMetadataStream = personMetadataStream
        .flatMapValues((k, personMetadata) -> getPersonMetadataUnpackedForAllHierarchy(personMetadata))
        .flatMapValues((k, personMetadata) -> getUnpackedPersonMetadataPerAncestorList(personMetadata))
        .flatMapValues((k, personMetadata) -> getUnpackedPersonMetadataPerMetaItemList(personMetadata))
        .selectKey((k, personMetadata) -> getPlanAncestorHierarchyKey(personMetadata));

    KGroupedStream<String, PersonMetadataUnpackedEvent> stringLocationMetadataUnpackedEventKGroupedStream = unpackedPersonMetadataStream
        .selectKey((k, v) -> k)
        .groupByKey(
            Grouped.with(Serdes.String(), new JsonSerde<>(PersonMetadataUnpackedEvent.class)));

    stringLocationMetadataUnpackedEventKGroupedStream
        .aggregate(PersonBusinessStatusAggregate::new,
            (key, personMetadataUnpackedEvent, aggregate) -> getPersonMetadataAggregate(personMetadataUnpackedEvent, aggregate),
            Materialized.<String, PersonBusinessStatusAggregate, KeyValueStore<Bytes, byte[]>>as(
                    kafkaProperties.getStoreMap().get(KafkaConstants.personBusinessStatus))
                .withValueSerde(new JsonSerde<>(PersonBusinessStatusAggregate.class))
                .withKeySerde(Serdes.String())
        );
    return personMetadataStream;
  }

  @Bean
  KStream<UUID, PersonMetadataEvent> personMetadataStructureProcessor(StreamsBuilder streamsBuilder) {

    KStream<UUID, PersonMetadataEvent> personMetadataStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(KafkaConstants.PERSON_METADATA_UPDATE),
        Consumed.with(Serdes.UUID(), new JsonSerde<>(PersonMetadataEvent.class)));
    personMetadataStream.peek(
        (k, v) -> streamLog.debug("personMetadataStream - k: {} v: {}", k, v));

    KStream<String, LocationPersonBusinessStateStreamTransportEvent> personBusinessStateLocationMetaDataUnpacked = personMetadataStream
        .filter((k, personMetadataEvent) -> personMetadataEvent.getLocationIdList() != null
            && !personMetadataEvent.getLocationIdList().isEmpty())
        .flatMapValues((k, personMetadataEvent) ->
            personMetadataEvent.getLocationIdList().stream()
                .map((locationId) -> {
                  LocationPersonBusinessStateStreamTransportEvent locationPersonBusinessStateStreamTransportEvent = new LocationPersonBusinessStateStreamTransportEvent();
                  locationPersonBusinessStateStreamTransportEvent.setLocationId(locationId);
                  locationPersonBusinessStateStreamTransportEvent.setMetaDataEvents(
                      personMetadataEvent.getMetaDataEvents());
                  locationPersonBusinessStateStreamTransportEvent.setPersonIdentifier(personMetadataEvent.getEntityId());
                  return locationPersonBusinessStateStreamTransportEvent;
                }).collect(Collectors.toList())
        )
        .flatMapValues((k, locationPersonBusinessStateStreamTransportEventStream) ->
            locationPersonBusinessStateStreamTransportEventStream.getMetaDataEvents().stream()
                .map(metaDataEvent -> {
                  LocationPersonBusinessStateStreamTransportEvent locationPersonBusinessStateStreamTransportEvent = new LocationPersonBusinessStateStreamTransportEvent();
                  locationPersonBusinessStateStreamTransportEvent.setPersonIdentifier(
                      locationPersonBusinessStateStreamTransportEventStream.getPersonIdentifier());
                  locationPersonBusinessStateStreamTransportEvent.setPersonBusinessState(
                      metaDataEvent.getTagData().getValue().getValueString());
                  locationPersonBusinessStateStreamTransportEvent.setUpdateTime(
                      metaDataEvent.getTagData().getMeta().getUpdateDateTime());
                  locationPersonBusinessStateStreamTransportEvent.setLocationId(
                      locationPersonBusinessStateStreamTransportEventStream.getLocationId());
                  locationPersonBusinessStateStreamTransportEvent.setPlanIdentifier(metaDataEvent.getTagData().getMeta().getPlanId());

                  locationPersonBusinessStateStreamTransportEvent.setMetadata(metaDataEvent.getTagData().getMeta());

                  return locationPersonBusinessStateStreamTransportEvent;
                }).collect(Collectors.toList())
        )
        .selectKey((k, locationPersonBusinessStateStreamTransportEventStream) ->
            locationPersonBusinessStateStreamTransportEventStream.getPlanIdentifier() + "_"
                + locationPersonBusinessStateStreamTransportEventStream.getLocationId());

    personBusinessStateLocationMetaDataUnpacked.peek(
        (k, v) -> streamLog.debug("personBusinessStateLocationMetaDataUnpacked - k: {} v: {}", k, v));

    KTable<String, LocationPersonBusinessStateAggregate> aggregate = personBusinessStateLocationMetaDataUnpacked
        .selectKey((k, v) -> k)
        .groupByKey(Grouped.with(Serdes.String(),
            new JsonSerde<>(LocationPersonBusinessStateStreamTransportEvent.class)))

        //aggregates on  plan id and location
        .aggregate(LocationPersonBusinessStateAggregate::new,
            (k, locationPersonBusinessStateStreamTransportEventStream, locationPersonBusinessStateAggregate) -> {

          if (locationPersonBusinessStateAggregate.getPersonBusinessStatusMap().isEmpty()){

            Map<UUID, PersonBusinessStatus> personBusinessStatusMap = new HashMap<>();

            PersonBusinessStatus personBusinessStatus = new PersonBusinessStatus();
            personBusinessStatus.setStatus(
                locationPersonBusinessStateStreamTransportEventStream.getPersonBusinessState());
            personBusinessStatus.setUpdateDateTime(
                locationPersonBusinessStateStreamTransportEventStream.getUpdateTime());
            personBusinessStatus.setUpdatingTaskType(locationPersonBusinessStateStreamTransportEventStream.getMetadata().getTaskType());

            personBusinessStatusMap.put(
                locationPersonBusinessStateStreamTransportEventStream.getPersonIdentifier(),
                personBusinessStatus);

            locationPersonBusinessStateAggregate.setPersonBusinessStatusMap(
                personBusinessStatusMap);
          } else {
            Map<UUID, PersonBusinessStatus> personBusinessStatusMap = locationPersonBusinessStateAggregate.getPersonBusinessStatusMap();
            PersonBusinessStatus personBusinessStatus;
            if (!locationPersonBusinessStateAggregate.getPersonBusinessStatusMap()
                .containsKey(
                    locationPersonBusinessStateStreamTransportEventStream.getPersonIdentifier())) {
              personBusinessStatus = new PersonBusinessStatus();
              personBusinessStatus.setStatus(
                  locationPersonBusinessStateStreamTransportEventStream.getPersonBusinessState());
              personBusinessStatus.setUpdateDateTime(locationPersonBusinessStateStreamTransportEventStream.getUpdateTime());
              personBusinessStatus.setUpdatingTaskType(locationPersonBusinessStateStreamTransportEventStream.getMetadata().getTaskType());

            } else {
              personBusinessStatus = personBusinessStatusMap
                  .get(
                      locationPersonBusinessStateStreamTransportEventStream.getPersonIdentifier());

              // Only allow the state change if the updating task is not MDA Adherence OR
              // if it is MDA Adherence, then the state from MDA Adherence must not be "Not Visited" and the previous updating task must be MDA Dispense
              // ( (thisTask.MDA_Adherence && !Not Visited) && prevTask.MDA_Dispense ) || (!thisTask.MDA_Adherence)
              //previous updating task
              if (!locationPersonBusinessStateStreamTransportEventStream.getMetadata().getTaskType()
                  .equals("MDA Adherence")
                  || !locationPersonBusinessStateStreamTransportEventStream.getPersonBusinessState()
                  .equals("Not Visited")) {
                if (locationPersonBusinessStateStreamTransportEventStream.getUpdateTime()
                    .isAfter(personBusinessStatus.getUpdateDateTime())) {
                  personBusinessStatus.setStatus(
                      locationPersonBusinessStateStreamTransportEventStream.getPersonBusinessState());
                  personBusinessStatus.setUpdateDateTime(
                      locationPersonBusinessStateStreamTransportEventStream.getUpdateTime());
                }
              }
            }
            personBusinessStatusMap.put(
                locationPersonBusinessStateStreamTransportEventStream.getPersonIdentifier(),
                personBusinessStatus);
            locationPersonBusinessStateAggregate.setPersonBusinessStatusMap(
                personBusinessStatusMap);
          }

              return locationPersonBusinessStateAggregate;
            }
            ,
            Materialized.<String, LocationPersonBusinessStateAggregate, KeyValueStore<Bytes, byte[]>>as(
                    kafkaProperties.getStoreMap().get(KafkaConstants.structurePeople)
                ).withKeySerde(Serdes.String())
                .withValueSerde(new JsonSerde<>(LocationPersonBusinessStateAggregate.class))
        );
    aggregate.toStream().peek(
        (k, v) -> streamLog.debug("aggregate.toStream() - k: {} v: {}", k, v));

    KStream<String, LocationPersonBusinessStateAggregate> stringLocationPersonBusinessStateAggregateKStream = aggregate
        .toStream();

    KStream<String, String> stringString = stringLocationPersonBusinessStateAggregateKStream
        .flatMapValues((k,locationPersonBusinessStateAggregate) -> locationPersonBusinessStateAggregate.getPersonBusinessStatusMap()
            .entrySet()
            .stream()
            .map(entry -> entry.getValue().getStatus() + "_" + entry.getKey().toString()).collect(
                Collectors.toList()));

    stringString.peek(
        (k, v) -> streamLog.debug("stringString - k: {} v: {}", k, v));

    KGroupedStream<String, String> stringStringKGroupedStream = stringString
        .groupByKey(Grouped.with(Serdes.String(), Serdes.String()));

    KTable<String, LocationPersonBusinessStateCountAggregate> locationPersonAggregate = stringStringKGroupedStream
        .aggregate(LocationPersonBusinessStateCountAggregate::new,
            (k, v, agg) -> getLocationPersonCountAggregate(v, agg)
              ,Materialized.<String, LocationPersonBusinessStateCountAggregate, KeyValueStore<Bytes, byte[]>>as(kafkaProperties.getStoreMap().get(KafkaConstants.structurePeopleCounts))
                .withValueSerde(new JsonSerde<>(LocationPersonBusinessStateCountAggregate.class))
                .withKeySerde(Serdes.String()));

    locationPersonAggregate.toStream().peek((k, v) -> streamLog.debug("locationPersonAggregate.toStream() - k: {} v: {}", k, v));

    return personMetadataStream;
  }

  private LocationPersonBusinessStateCountAggregate getLocationPersonCountAggregate(String v, LocationPersonBusinessStateCountAggregate agg) {
    String newBusinessStatus = v.split("_")[0];
    String personIdentifier = v.split("_")[1];

    Map<String, String> personBusinessStatusMap = agg.getPersonBusinessStatusMap();
    Map<String, List<String>> structureBusinessStatePersonMap = agg.getStructureBusinessStatePersonMap();

    List<String> updatedPersonList;

    if (personBusinessStatusMap.containsKey(personIdentifier)) {

      String currentBusinessStatus = personBusinessStatusMap.get(personIdentifier);
      personBusinessStatusMap.remove(personIdentifier);

      List<String> personList = structureBusinessStatePersonMap.get(
          currentBusinessStatus);
      personList.remove(personIdentifier);

      if (structureBusinessStatePersonMap.containsKey(newBusinessStatus)) {
        updatedPersonList = structureBusinessStatePersonMap.
            get(newBusinessStatus);
        updatedPersonList.add(personIdentifier);
      } else {
        updatedPersonList = new ArrayList<>();
        updatedPersonList.add(personIdentifier);
      }

      structureBusinessStatePersonMap.put(currentBusinessStatus, personList);

    } else {

      if (structureBusinessStatePersonMap.containsKey(newBusinessStatus)) {
        updatedPersonList = structureBusinessStatePersonMap.
            get(newBusinessStatus);
        updatedPersonList.add(personIdentifier);
      } else {
        updatedPersonList = new ArrayList<>();
        updatedPersonList.add(personIdentifier);
      }

    }
    structureBusinessStatePersonMap.put(newBusinessStatus, updatedPersonList);
    personBusinessStatusMap.put(personIdentifier, newBusinessStatus);

    agg.setPersonBusinessStatusMap(personBusinessStatusMap);
    agg.setStructureBusinessStatePersonMap(structureBusinessStatePersonMap);

    Map<String, List<String>> structureBusinessStatePersonMap1 = agg.getStructureBusinessStatePersonMap();

    Map<String, Long> counts = agg.getStructureBusinessStatePersonMap().entrySet()
        .stream()
        .map(entry -> new SimpleEntry<>(entry.getKey(),
            (long) entry.getValue().size()))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    agg.setStructureBusinessStateCountMap(counts);

    return agg;
  }

  private PersonBusinessStatusAggregate getPersonMetadataAggregate(
      PersonMetadataUnpackedEvent personMetadataUnpackedEvent, PersonBusinessStatusAggregate aggregate) {

    if (personMetadataUnpackedEvent.getMetaDataEvent().isActive()) {
      Map<UUID, CurrentState> personState;

      if (!aggregate.isInitialized()) {
        //Create a new map for this hierarchy
        CurrentState currentState = new CurrentState();
        currentState.setState(personMetadataUnpackedEvent.getMetaDataEvent().getTagData().getValue()
            .getValueString());
        currentState.setUpdateTime(
            personMetadataUnpackedEvent.getMetaDataEvent().getTagData().getMeta()
                .getUpdateDateTime());

        personState = new HashMap<>();
        personState.put(personMetadataUnpackedEvent.getEntityId(), currentState);

        aggregate.setInitialized(true);
        aggregate.setLocationsTreated(new ArrayList<>());
        aggregate.setPersonTreated(new ArrayList<>());
        aggregate.setPersonEligible(new ArrayList<>());

      } else {
        personState = aggregate.getPersonState();
        if (personState.containsKey(personMetadataUnpackedEvent.getEntityId())) {

          if (!personMetadataUnpackedEvent.getMetaDataEvent().getTagData().getMeta().getTaskType()
              .equals("MDA Adherence") || !personMetadataUnpackedEvent.getMetaDataEvent()
              .getTagData().getValue()
              .getValueString().equals("Not Visited")) {

            if (personState.get(personMetadataUnpackedEvent.getEntityId()).getUpdateTime().isBefore(
                personMetadataUnpackedEvent.getMetaDataEvent().getTagData().getMeta()
                    .getUpdateDateTime())) {
              //Update existing person in map
              personState.get(personMetadataUnpackedEvent.getEntityId()).setState(
                  personMetadataUnpackedEvent.getMetaDataEvent().getTagData().getValue()
                      .getValueString());
              personState.get(personMetadataUnpackedEvent.getEntityId()).setUpdateTime(
                  personMetadataUnpackedEvent.getMetaDataEvent().getTagData().getMeta()
                      .getUpdateDateTime());
            }
          }

        } else {
          //Add new person to existing map
          CurrentState currentState = new CurrentState();
          currentState.setState(
              personMetadataUnpackedEvent.getMetaDataEvent().getTagData().getValue()
                  .getValueString());
          currentState.setUpdateTime(
              personMetadataUnpackedEvent.getMetaDataEvent().getTagData().getMeta()
                  .getUpdateDateTime());

          personState.put(personMetadataUnpackedEvent.getEntityId(), currentState);
        }
      }
      //TODO: fix hardcodings
      aggregate.setPersonState(personState);
      if (personState.get(personMetadataUnpackedEvent.getEntityId()).getState()
          .equals("SMC Complete") || personState.get(personMetadataUnpackedEvent.getEntityId())
          .getState().equals("SPAQ Complete")) {
        if (!aggregate.getLocationsTreated()
            .contains(personMetadataUnpackedEvent.getThisLocationId())) {
          List<UUID> locationsTreated = aggregate.getLocationsTreated();
          locationsTreated.add(personMetadataUnpackedEvent.getThisLocationId());
          aggregate.setLocationsTreated(locationsTreated);
        }
        //TODO: Ideally the persons age should be available here so that we can check if they are in the date range
        if (!aggregate.getPersonTreated().contains(personMetadataUnpackedEvent.getEntityId())) {
          List<UUID> personTreated = aggregate.getPersonTreated();
          personTreated.add(personMetadataUnpackedEvent.getEntityId());
          aggregate.setPersonTreated(personTreated);
        }
      }
      if (!personState.get(personMetadataUnpackedEvent.getEntityId())
          .getState().equals("Ineligible")) {
        if (!aggregate.getPersonEligible().contains(personMetadataUnpackedEvent.getEntityId())) {
          List<UUID> personEligible = aggregate.getPersonEligible();
          personEligible.add(personMetadataUnpackedEvent.getEntityId());
          aggregate.setPersonEligible(personEligible);
        }
      }
    } else {
      aggregate = null;
    }
    return aggregate;
  }

  private String getPlanAncestorHierarchyKey(PersonMetadataUnpackedEvent personMetadata) {
    return personMetadata.getMetaDataEvent().getTagData().getMeta().getPlanId() + "_" +
        personMetadata.getAncestorNode() + "_" +
        personMetadata.getHierarchyIdentifier();
  }

  private List<PersonMetadataUnpackedEvent> getUnpackedPersonMetadataPerMetaItemList(PersonMetadataEvent personMetadata) {
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

  private List<PersonMetadataEvent> getUnpackedPersonMetadataPerAncestorList(PersonMetadataEvent personMetadata) {
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

  private PersonMetadataEvent getPersonMetadataEventWithAncestorNode(PersonMetadataEvent personMetadata,
      UUID locationId, UUID ancestorNode) {
    PersonMetadataEvent locationMetadataEvent = new PersonMetadataEvent();
    locationMetadataEvent.setHierarchyIdentifier(
        personMetadata.getHierarchyIdentifier());
    locationMetadataEvent.setMetaDataEvents(personMetadata.getMetaDataEvents());
    locationMetadataEvent.setEntityId(personMetadata.getEntityId());
    locationMetadataEvent.setThisLocation(locationId);
    locationMetadataEvent.setAncestorNode(ancestorNode);
    return locationMetadataEvent;
  }

  private List<PersonMetadataEvent> getPersonMetadataUnpackedForAllHierarchy(PersonMetadataEvent personMetadata) {
    return locationHierarchyService.getAll()
        .stream()
        .map(locationHierarchy -> getPersonMetadataEventWithLocationHierarchy(personMetadata,
            locationHierarchy)).collect(Collectors.toList());
  }

  private PersonMetadataEvent getPersonMetadataEventWithLocationHierarchy(PersonMetadataEvent personMetadata,
      LocationHierarchy locationHierarchy) {
    PersonMetadataEvent personMetadataEvent = new PersonMetadataEvent();
    personMetadataEvent.setHierarchyIdentifier(locationHierarchy.getIdentifier());
    personMetadataEvent.setMetaDataEvents(personMetadata.getMetaDataEvents());
    personMetadataEvent.setEntityId(personMetadata.getEntityId());
    personMetadataEvent.setLocationIdList(personMetadata.getLocationIdList());
    return personMetadataEvent;
  }


}
