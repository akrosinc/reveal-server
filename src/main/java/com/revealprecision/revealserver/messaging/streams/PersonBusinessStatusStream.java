package com.revealprecision.revealserver.messaging.streams;

import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.PersonBusinessStatus;
import com.revealprecision.revealserver.messaging.message.PersonBusinessStatus.CurrentState;
import com.revealprecision.revealserver.messaging.message.PersonMetadataEvent;
import com.revealprecision.revealserver.messaging.message.PersonMetadataUnpackedEvent;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.LocationHierarchyService;
import com.revealprecision.revealserver.service.LocationRelationshipService;
import com.revealprecision.revealserver.service.LocationService;
import com.revealprecision.revealserver.service.PersonService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class PersonBusinessStatusStream {


  private final KafkaProperties kafkaProperties;
  private final LocationService locationService;
  private final LocationRelationshipService locationRelationshipService;
  private final LocationHierarchyService locationHierarchyService;
  private final PersonService personService;

  @Bean
  KStream<UUID, PersonMetadataEvent> getPersonRelationships2(StreamsBuilder streamsBuilder) {

    //person imported
    KStream<UUID, PersonMetadataEvent> personMetadataStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(KafkaConstants.PERSON_METADATA_UPDATE),
        Consumed.with(Serdes.UUID(), new JsonSerde<>(PersonMetadataEvent.class)));
//    locationMetadataStream.print(Printed.<UUID, LocationMetadataEvent>toSysOut());

    KStream<String, PersonMetadataUnpackedEvent> unpackedPersonMetadataStream = personMetadataStream
        .flatMapValues((k, personMetadata) -> {
          List<PersonMetadataEvent> personMetadataEventsWithHierarchy = locationHierarchyService.getAll()
              .stream()
              .map(locationHierarchy -> {
                PersonMetadataEvent personMetadataEvent = new PersonMetadataEvent();
                personMetadataEvent.setHierarchyIdentifier(locationHierarchy.getIdentifier());
                personMetadataEvent.setMetaDataEvents(personMetadata.getMetaDataEvents());
                personMetadataEvent.setEntityId(personMetadata.getEntityId());
                personMetadataEvent.setLocationIdList(personMetadata.getLocationIdList());

                return personMetadataEvent;
              }).collect(Collectors.toList());
          return personMetadataEventsWithHierarchy;
        })
        .flatMapValues((k, personMetadata) -> {
          return personMetadata.getLocationIdList().stream().flatMap(locationId -> {
            LocationRelationship locationRelationShip = locationRelationshipService.getLocationRelationshipsForLocation(
                personMetadata.getHierarchyIdentifier(), locationId);
            return locationRelationShip.getAncestry()
                .stream()
                .map(ancestorNode -> {
                  PersonMetadataEvent locationMetadataEvent = new PersonMetadataEvent();
                  locationMetadataEvent.setHierarchyIdentifier(
                      personMetadata.getHierarchyIdentifier());
                  locationMetadataEvent.setMetaDataEvents(personMetadata.getMetaDataEvents());
                  locationMetadataEvent.setEntityId(personMetadata.getEntityId());
                  locationMetadataEvent.setThisLocation(locationId);
                  locationMetadataEvent.setAncestorNode(ancestorNode);
                  return locationMetadataEvent;
                });
          }).collect(Collectors.toList());
        })
        .flatMapValues((k, personMetadata) -> {
          List<PersonMetadataUnpackedEvent> personMetadataEventsWithHierarchyAndAncestorNodeWithIndividualMetadata = personMetadata.getMetaDataEvents()
              .stream().map(metaDataEvent -> {
                PersonMetadataUnpackedEvent locationMetadataUnpackedEvent = new PersonMetadataUnpackedEvent();
                locationMetadataUnpackedEvent.setHierarchyIdentifier(
                    personMetadata.getHierarchyIdentifier());
                locationMetadataUnpackedEvent.setEntityId(personMetadata.getEntityId());
                locationMetadataUnpackedEvent.setAncestorNode(personMetadata.getAncestorNode());
                locationMetadataUnpackedEvent.setMetaDataEvent(metaDataEvent);
                locationMetadataUnpackedEvent.setThisLocationId(personMetadata.getThisLocation());
                return locationMetadataUnpackedEvent;
              }).collect(Collectors.toList());
          return personMetadataEventsWithHierarchyAndAncestorNodeWithIndividualMetadata;
        }).selectKey((k, v) -> {
          return v.getMetaDataEvent().getTagData().getMeta().getPlanId() + "_" +
              v.getAncestorNode() + "_" +
              v.getHierarchyIdentifier();
        });

    KGroupedStream<String, PersonMetadataUnpackedEvent> stringLocationMetadataUnpackedEventKGroupedStream = unpackedPersonMetadataStream
        .selectKey((k, v) -> k)
        .groupByKey(
            Grouped.with(Serdes.String(), new JsonSerde<>(PersonMetadataUnpackedEvent.class)));

    KTable<String, PersonBusinessStatus> aggregate1 = stringLocationMetadataUnpackedEventKGroupedStream
        .aggregate(PersonBusinessStatus::new,
            (key, personMetadataUnpackedEvent, aggregate) -> {

              if (personMetadataUnpackedEvent.getMetaDataEvent().isActive()) {
                Map<UUID, CurrentState> personState;

                if (!aggregate.isInitialized()) {
                  //Create a new map for this hierarchy
                  CurrentState currentState = new CurrentState();
                  currentState.setState(
                      personMetadataUnpackedEvent.getMetaDataEvent().getTagData().getValue()
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
                  if (personState
                      .containsKey(personMetadataUnpackedEvent.getEntityId())) {
                    if (personState.get(personMetadataUnpackedEvent.getEntityId())
                        .getUpdateTime().isBefore(
                            personMetadataUnpackedEvent.getMetaDataEvent().getTagData().getMeta()
                                .getUpdateDateTime())) {
                      //Update existing person in map

                      personState.get(personMetadataUnpackedEvent.getEntityId())
                          .setState(
                              personMetadataUnpackedEvent.getMetaDataEvent().getTagData().getValue()
                                  .getValueString());
                      personState.get(personMetadataUnpackedEvent.getEntityId())
                          .setUpdateTime(
                              personMetadataUnpackedEvent.getMetaDataEvent().getTagData().getMeta()
                                  .getUpdateDateTime());
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
                aggregate.setPersonState(personState);
                if (personState.get(personMetadataUnpackedEvent.getEntityId()).getState()
                    .equals("SMC Complete")
                    || personState.get(personMetadataUnpackedEvent.getEntityId()).getState()
                    .equals("SPAQ Complete")) {
                  if (!aggregate.getLocationsTreated()
                      .contains(personMetadataUnpackedEvent.getThisLocationId())) {
                    List<UUID> locationsTreated = aggregate.getLocationsTreated();
                    locationsTreated.add(personMetadataUnpackedEvent.getThisLocationId());
                    aggregate.setLocationsTreated(locationsTreated);
                  }
                  //TODO: Ideally the persons age should be available here so that we can check if they are in the date range
                  if (!aggregate.getPersonTreated()
                      .contains(personMetadataUnpackedEvent.getEntityId())) {
                    List<UUID> personTreated = aggregate.getPersonTreated();
                    personTreated.add(personMetadataUnpackedEvent.getEntityId());
                    aggregate.setPersonTreated(personTreated);
                  }
                }
                if (!personState.get(personMetadataUnpackedEvent.getEntityId()).getState()
                    .equals("Not Visited") && (!personState.get(
                        personMetadataUnpackedEvent.getEntityId()).getState()
                    .equals("Ineligible"))) {
                  if (!aggregate.getPersonEligible()
                      .contains(personMetadataUnpackedEvent.getEntityId())) {
                    List<UUID> personEligible = aggregate.getPersonEligible();
                    personEligible.add(personMetadataUnpackedEvent.getEntityId());
                    aggregate.setPersonEligible(personEligible);
                  }
                }
              } else {
                aggregate = null;
              }
              return aggregate;

            },
            Materialized.<String, PersonBusinessStatus, KeyValueStore<Bytes, byte[]>>as(
                    kafkaProperties.getStoreMap().get(KafkaConstants.personBusinessStatus))
                .withValueSerde(new JsonSerde<>(PersonBusinessStatus.class))
                .withKeySerde(Serdes.String())
        );
    return personMetadataStream;
  }


}
