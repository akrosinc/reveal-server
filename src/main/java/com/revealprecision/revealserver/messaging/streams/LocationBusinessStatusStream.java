package com.revealprecision.revealserver.messaging.streams;

import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.messaging.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.LocationBusinessStatusAggregate;
import com.revealprecision.revealserver.messaging.message.LocationMetadataContainer;
import com.revealprecision.revealserver.messaging.message.LocationMetadataEvent;
import com.revealprecision.revealserver.messaging.message.LocationMetadataUnpackedEvent;
import com.revealprecision.revealserver.messaging.message.OperationalAreaAggregate;
import com.revealprecision.revealserver.messaging.message.OperationalAreaVisitedCount;
import com.revealprecision.revealserver.messaging.message.OperationalAreaVisitedCount.IndividualOperationalAreaCountsByBusinessStatus;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.props.BusinessStatusProperties;
import com.revealprecision.revealserver.props.DashboardProperties;
import com.revealprecision.revealserver.props.KafkaProperties;
import com.revealprecision.revealserver.service.LocationHierarchyService;
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
public class LocationBusinessStatusStream {


  private final KafkaProperties kafkaProperties;
  private final LocationRelationshipService locationRelationshipService;
  private final LocationHierarchyService locationHierarchyService;
  private final BusinessStatusProperties businessStatusProperties;
  private final DashboardProperties dashboardProperties;
  private final LocationService locationService;
  private final PlanService planService;
  private final Logger streamLog = LoggerFactory.getLogger("stream-file");

  @Bean
  KStream<UUID, LocationMetadataEvent> locationBusinessStatusCountsAggregator(
      StreamsBuilder streamsBuilder) {

    KStream<UUID, LocationMetadataEvent> locationMetadataStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(KafkaConstants.LOCATION_METADATA_UPDATE),
        Consumed.with(Serdes.UUID(), new JsonSerde<>(LocationMetadataEvent.class)));

    locationMetadataStream.peek(
        (k, v) -> streamLog.debug("locationMetadataStream - k: {} v: {}", k, v));

    KStream<String, LocationMetadataUnpackedEvent> unpackedLocationMetadataStream = locationMetadataStream
        .flatMapValues((k, locationMetadata) -> getLocationMetadataUnpackedByLocationHierarchy(
            locationMetadata))
        .flatMapValues(
            (k, locationMetadata) -> getLocationMetadataUnpackedByAncestry(locationMetadata))
        .flatMapValues(
            (k, locationMetadata) -> getLocationMetadataUnpackedByMetadataItems(locationMetadata))
        .selectKey((k, locationMetadata) -> getPlanAncestorHierarchyEntityKey(locationMetadata));

    unpackedLocationMetadataStream.peek(
        (k, v) -> streamLog.debug("unpackedLocationMetadataStream - k: {} v: {}", k, v));

    KGroupedStream<String, LocationMetadataUnpackedEvent> stringLocationMetadataUnpackedEventKGroupedStream = unpackedLocationMetadataStream
        .selectKey((k, v) -> k)
        .groupByKey(
            Grouped.with(Serdes.String(), new JsonSerde<>(LocationMetadataUnpackedEvent.class)));

    KTable<String, LocationBusinessStatusAggregate> individualLocationByBusinessStatusTable = stringLocationMetadataUnpackedEventKGroupedStream
        .aggregate(LocationBusinessStatusAggregate::new,
            (key, locationMetadataUnpackedEvent, aggregate) -> getLocationMetadataAggregate(
                locationMetadataUnpackedEvent, aggregate),
            Materialized.<String, LocationBusinessStatusAggregate, KeyValueStore<Bytes, byte[]>>as(
                    kafkaProperties.getStoreMap().get(KafkaConstants.locationBusinessStatus))
                .withValueSerde(new JsonSerde<>(LocationBusinessStatusAggregate.class))
                .withKeySerde(Serdes.String()));

    individualLocationByBusinessStatusTable
        .groupBy((k, locationBusinessStatusAggregate) ->
                KeyValue.pair(
                    getPlanAncestorHierarchyBusinessStatusKey(k, locationBusinessStatusAggregate),
                    locationBusinessStatusAggregate),
            Grouped.with(Serdes.String(), new JsonSerde<>(LocationBusinessStatusAggregate.class)))
        .count(Materialized.as(kafkaProperties.getStoreMap()
            .get(KafkaConstants.locationBusinessStatusByPlanParentHierarchy)));
    return locationMetadataStream;
  }

  private String getPlanAncestorHierarchyBusinessStatusKey(String k,
      LocationBusinessStatusAggregate locationBusinessStatusAggregate) {
    return k.split("_")[0] + "_" + //plan
        k.split("_")[1] + "_" +  //ancestor
        k.split("_")[2] + "_" + //hierarchy
        locationBusinessStatusAggregate.getBusinessStatus();//business_status
  }

  private LocationBusinessStatusAggregate getLocationMetadataAggregate(
      LocationMetadataUnpackedEvent locationMetadataUnpackedEvent,
      LocationBusinessStatusAggregate aggregate) {
    if (locationMetadataUnpackedEvent.getMetaDataEvent().isActive()) {
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
    } else {
      aggregate = null;
    }
    return aggregate;
  }

  private String getPlanAncestorHierarchyEntityKey(LocationMetadataUnpackedEvent v) {
    return v.getMetaDataEvent().getTagData().getMeta().getPlanId() + "_" +
        v.getAncestorNode() + "_" +
        v.getHierarchyIdentifier() + "_" +
        v.getEntityId();
  }

  private List<LocationMetadataUnpackedEvent> getLocationMetadataUnpackedByMetadataItems(
      LocationMetadataEvent locationMetadata) {
    return locationMetadata.getMetaDataEvents()
        .stream()
        .filter(metaDataEvent -> metaDataEvent.getType()
            .equals(businessStatusProperties.getBusinessStatusTagName()))
        .map(metaDataEvent -> {
          LocationMetadataUnpackedEvent locationMetadataUnpackedEvent = new LocationMetadataUnpackedEvent();
          locationMetadataUnpackedEvent.setHierarchyIdentifier(
              locationMetadata.getHierarchyIdentifier());
          locationMetadataUnpackedEvent.setEntityId(locationMetadata.getEntityId());
          locationMetadataUnpackedEvent.setAncestorNode(locationMetadata.getAncestorNode());
          locationMetadataUnpackedEvent.setMetaDataEvent(metaDataEvent);
          return locationMetadataUnpackedEvent;
        }).collect(Collectors.toList());
  }

  private List<LocationMetadataEvent> getLocationMetadataUnpackedByAncestry(
      LocationMetadataEvent locationMetadata) {
    LocationRelationship locationRelationShip = locationRelationshipService.getLocationRelationshipsForLocation(
        locationMetadata.getHierarchyIdentifier(), locationMetadata.getEntityId());
    return locationRelationShip.getAncestry()
        .stream()
        .map(ancestorNode -> getLocationMetadataEventPerAncestor(locationMetadata, ancestorNode))
        .collect(Collectors.toList());
  }

  private LocationMetadataEvent getLocationMetadataEventPerAncestor(
      LocationMetadataEvent locationMetadata,
      UUID ancestorNode) {
    LocationMetadataEvent locationMetadataEvent = new LocationMetadataEvent();
    locationMetadataEvent.setHierarchyIdentifier(
        locationMetadata.getHierarchyIdentifier());
    locationMetadataEvent.setMetaDataEvents(locationMetadata.getMetaDataEvents());
    locationMetadataEvent.setEntityId(locationMetadata.getEntityId());
    locationMetadataEvent.setAncestorNode(ancestorNode);
    return locationMetadataEvent;
  }

  private List<LocationMetadataEvent> getLocationMetadataUnpackedByLocationHierarchy(
      LocationMetadataEvent locationMetadata) {
    return locationHierarchyService.getAll()
        .stream()
        .map(locationHierarchy -> {
          LocationMetadataEvent locationMetadataEvent = new LocationMetadataEvent();
          locationMetadataEvent.setHierarchyIdentifier(locationHierarchy.getIdentifier());
          locationMetadataEvent.setMetaDataEvents(locationMetadata.getMetaDataEvents());
          locationMetadataEvent.setEntityId(locationMetadata.getEntityId());
          return locationMetadataEvent;
        }).collect(Collectors.toList());
  }

  @Bean
  KStream<UUID, LocationMetadataEvent> operationalAreaVisitedProcessor(
      StreamsBuilder streamsBuilder) {
    //TODO: remove hardcodings

    KStream<UUID, LocationMetadataEvent> locationMetadataStream = streamsBuilder.stream(
        kafkaProperties.getTopicMap().get(KafkaConstants.LOCATION_METADATA_UPDATE),
        Consumed.with(Serdes.UUID(), new JsonSerde<>(LocationMetadataEvent.class)));

    KStream<UUID, LocationMetadataContainer> locationMetadataUnpackedPerMetadataItems = locationMetadataStream
        .flatMapValues(
            (k, locationMetadata) -> getLocationMetadataUnpackedPerMetadataItems(locationMetadata))
        .selectKey((k, metadataEventContainer) -> metadataEventContainer.getLocationIdentifier());

    KStream<UUID, LocationMetadataContainer> locationMetadataUnpackedPerMetadataItemsWithAncestor = locationMetadataUnpackedPerMetadataItems
        .filter((k, metadataEventContainer) -> metadataEventContainer.getMetaDataEvent().getType()
            .equals("business-status"))
        .filter(
            (k, metadataEventContainer) -> locationService.findByIdentifier(k).getGeographicLevel()
                .getName().equals(LocationConstants.STRUCTURE))
        .mapValues((k, metadataEventContainer) -> getLocationMetadataContainerWithAncestry(
            metadataEventContainer));

    KTable<String, LocationBusinessStatusAggregate> latestBusinessStateAggregate = locationMetadataUnpackedPerMetadataItemsWithAncestor
        .groupBy(
            (k, metadataEventContainer) -> getLocationIdentifierPlanKey(metadataEventContainer))
        .aggregate(LocationBusinessStatusAggregate::new,
            (key, metadataEventContainer, aggregate) -> aggregateSetLatestBusinessStatusOnLocation(
                metadataEventContainer, aggregate),
            Materialized.<String, LocationBusinessStatusAggregate, KeyValueStore<Bytes, byte[]>>as(
                    kafkaProperties.getStoreMap()
                        .get(KafkaConstants.locationBusinessStatusForOperationalAreas))
                .withValueSerde(new JsonSerde<>(LocationBusinessStatusAggregate.class))
                .withKeySerde(Serdes.String()));

    KTable<String, OperationalAreaAggregate> latestBusinessStateAggregatePerOperationalArea = latestBusinessStateAggregate
        .groupBy((key, locationBusinessStatus) ->
                KeyValue.pair(getAncestryPlanKey(key, locationBusinessStatus), locationBusinessStatus),
            Grouped.with(Serdes.String(), new JsonSerde<>(LocationBusinessStatusAggregate.class)))
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
        );

    KTable<String, OperationalAreaAggregate> restructuredOperationalAreaAggregate = latestBusinessStateAggregatePerOperationalArea
        .toStream()
        .flatMapValues(this::getOperationalAreaAggregatesUnpackedByAncestry)
        .groupBy(this::getAncestorOperationalAreaPlanIdKey)
        .aggregate(OperationalAreaAggregate::new,
            (key, operationalAreaAggregate, aggregate) -> restructureOperationalAreaAggregate(
                operationalAreaAggregate, aggregate),
            Materialized.<String, OperationalAreaAggregate, KeyValueStore<Bytes, byte[]>>as(
                    kafkaProperties.getStoreMap().get(KafkaConstants.tableOfOperationalAreaHierarchies))
                .withValueSerde(new JsonSerde<>(OperationalAreaAggregate.class))
                .withKeySerde(Serdes.String()));

    restructuredOperationalAreaAggregate
        .toStream()
        .mapValues((k, operationalAreaAggregate) -> {
          operationalAreaAggregate.setIdentifier(UUID.fromString(k.split("_")[1]));
          return operationalAreaAggregate;
        })
        .groupBy((k, entry) -> getAncestryPlanKey(k))
        .aggregate((OperationalAreaVisitedCount::new),
            (k, operationalAreaAggregate, aggregate) -> getAggregatedOperationalAreaVisitedCount(
                operationalAreaAggregate, aggregate),
            Materialized.<String, OperationalAreaVisitedCount, KeyValueStore<Bytes, byte[]>>as(
                    kafkaProperties.getStoreMap()
                        .get(KafkaConstants.operationalAreaByPlanParentHierarchy))
                .withValueSerde(new JsonSerde<>(OperationalAreaVisitedCount.class))
                .withKeySerde(Serdes.String()));

    restructuredOperationalAreaAggregate.toStream().to(kafkaProperties.getTopicMap()
        .get(KafkaConstants.tableOfOperationalAreaHierarchiesTOPIC));

    return locationMetadataStream;
  }

  private OperationalAreaVisitedCount getAggregatedOperationalAreaVisitedCount(
      OperationalAreaAggregate operationalAreaAggregate, OperationalAreaVisitedCount aggregate) {
    Long sumOfStructures = operationalAreaAggregate.getAggregatedLocationCount().values()
        .stream().reduce(0L, Long::sum);
    Long notVisitedStructures = operationalAreaAggregate.getAggregatedLocationCount().entrySet()
        .stream().filter(entry -> entry.getKey().equals("Not Visited")).map(Entry::getValue)
        .reduce(0L, Long::sum);
    Long notEligibleStructures = operationalAreaAggregate.getAggregatedLocationCount().entrySet()
        .stream().filter(entry -> entry.getKey().equals("Not Eligible")).map(Entry::getValue)
        .reduce(0L, Long::sum);

    Long totalStructures = sumOfStructures - notEligibleStructures;

    log.trace("operational area: {} -  notVisitedStructures: {} / totalStructures: {} ",
        operationalAreaAggregate.getIdentifier(), notVisitedStructures, totalStructures);
    boolean operationalAreaIsVisited = false;
    if (totalStructures > 0) {

      if ((100 - ((double) notVisitedStructures / (double) totalStructures * 100))
          >= (double) dashboardProperties.getOperationalAreaVisitedThreshold()) {
        operationalAreaIsVisited = true;
      }
    }

    if (aggregate.getOperationalObj().containsKey(operationalAreaAggregate.getIdentifier())) {
      IndividualOperationalAreaCountsByBusinessStatus individualOperationalAreaCountsByBusinessStatus = aggregate.getOperationalObj()
          .get(operationalAreaAggregate.getIdentifier());
      individualOperationalAreaCountsByBusinessStatus.setCounts(
          operationalAreaAggregate.getAggregatedLocationCount());
      individualOperationalAreaCountsByBusinessStatus.setOperationalAreaIsVisited(
          operationalAreaIsVisited);
      aggregate.getOperationalObj().put(operationalAreaAggregate.getIdentifier(),
          individualOperationalAreaCountsByBusinessStatus);

    } else {
      IndividualOperationalAreaCountsByBusinessStatus operationalAreaVisitedCount3 = new IndividualOperationalAreaCountsByBusinessStatus();
      operationalAreaVisitedCount3.setCounts(operationalAreaAggregate.getAggregatedLocationCount());
      operationalAreaVisitedCount3.setOperationalAreaIsVisited(operationalAreaIsVisited);
      aggregate.getOperationalObj()
          .put(operationalAreaAggregate.getIdentifier(), operationalAreaVisitedCount3);
    }

    long countOfVisitedOperationalAreas = aggregate.getOperationalObj().entrySet().stream()
        .filter(entry -> entry.getValue().isOperationalAreaIsVisited()).count();
    aggregate.setOperationalAreaVisitedCount(countOfVisitedOperationalAreas);

    return aggregate;
  }

  private String getAncestryPlanKey(String k) {
    return k.split("_")[0] + "_" + k.split("_")[k.split("_").length - 1];
  }

  private OperationalAreaAggregate restructureOperationalAreaAggregate(
      OperationalAreaAggregate operationalAreaAggregate, OperationalAreaAggregate aggregate) {
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
  }

  private String getAncestorOperationalAreaPlanIdKey(String k,
      OperationalAreaAggregate operationalAreaAggregate) {
    return operationalAreaAggregate.getAncestorIdentifier() + "_"
        + operationalAreaAggregate.getIdentifier() + "_" + k.split("_")[k.split("_").length - 1];
  }

  private List<OperationalAreaAggregate> getOperationalAreaAggregatesUnpackedByAncestry(String k,
      OperationalAreaAggregate operationalAreaAggregate) {
    List<String> ancestry = new ArrayList<>(List.of(k.split("_")));
    String remove = ancestry.remove(0);
    return ancestry.stream().map(
        ancestor -> getOperationalAreaAggregateWithAncestor(operationalAreaAggregate, remove,
            ancestor)).collect(Collectors.toList());
  }

  private OperationalAreaAggregate getOperationalAreaAggregateWithAncestor(
      OperationalAreaAggregate operationalAreaAggregate, String remove, String ancestor) {
    OperationalAreaAggregate operationalAreaAggregateConstructed = new OperationalAreaAggregate();
    operationalAreaAggregateConstructed.setAggregate(
        operationalAreaAggregate.getAggregate());
    operationalAreaAggregateConstructed.setAncestorIdentifier(UUID.fromString(ancestor));
    operationalAreaAggregateConstructed.setIdentifier(UUID.fromString(remove));
    return operationalAreaAggregateConstructed;
  }

  private String getAncestryPlanKey(String key,
      LocationBusinessStatusAggregate locationBusinessStatus) {
    return locationBusinessStatus.getAncestry().stream().map(UUID::toString)
        .collect(Collectors.joining("_")) + "_" + key.split("_")[key.split("_").length - 1];
  }

  private String getLocationIdentifierPlanKey(LocationMetadataContainer metadataEventContainer) {
    return metadataEventContainer.getLocationIdentifier().toString()
        .concat("_")
        .concat(metadataEventContainer.getMetaDataEvent().getTagData().getMeta().getPlanId()
            .toString());
  }

  private List<LocationMetadataContainer> getLocationMetadataUnpackedPerMetadataItems(
      LocationMetadataEvent locationMetadata) {
    return locationMetadata.getMetaDataEvents().stream()
        .map(metaDataEvent -> new LocationMetadataContainer(locationMetadata.getEntityId(),
            metaDataEvent, null)).collect(
            Collectors.toList());
  }

  private LocationBusinessStatusAggregate aggregateSetLatestBusinessStatusOnLocation(
      LocationMetadataContainer metadataEventContainer, LocationBusinessStatusAggregate aggregate) {
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
  }

  private LocationMetadataContainer getLocationMetadataContainerWithAncestry(
      LocationMetadataContainer metadataEventContainer) {
    Plan plan = planService.findPlanByIdentifier(
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
  }
}
