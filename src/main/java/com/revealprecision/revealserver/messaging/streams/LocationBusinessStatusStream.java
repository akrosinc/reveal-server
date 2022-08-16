package com.revealprecision.revealserver.messaging.streams;

import com.revealprecision.revealserver.constants.LocationConstants;
import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.messaging.message.LocationBusinessStatusAggregate;
import com.revealprecision.revealserver.messaging.message.LocationMetadataContainer;
import com.revealprecision.revealserver.messaging.message.LocationMetadataEvent;
import com.revealprecision.revealserver.messaging.message.LocationMetadataUnpackedEvent;
import com.revealprecision.revealserver.messaging.message.LocationStructureBusinessStatusAggregate;
import com.revealprecision.revealserver.messaging.message.OperationalAreaAggregate;
import com.revealprecision.revealserver.messaging.message.OperationalAreaVisitedCount;
import com.revealprecision.revealserver.messaging.message.OperationalAreaVisitedCount.IndividualOperationalAreaCountsByBusinessStatus;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.props.BusinessStatusProperties;
import com.revealprecision.revealserver.props.DashboardProperties;
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
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@Slf4j
@RequiredArgsConstructor
@Profile("Reveal-Streams")
public class LocationBusinessStatusStream {


  private final KafkaProperties kafkaProperties;
  private final LocationRelationshipService locationRelationshipService;
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

    KStream<UUID, LocationMetadataUnpackedEvent> uuidLocationMetadataUnpackedEventKStream = locationMetadataStream
        .flatMapValues(
            (k, locationMetadata) -> getLocationMetadataUnpackedByMetadataItems(locationMetadata));

    KStream<String, LocationMetadataUnpackedEvent> unpackedLocationMetadataStream = uuidLocationMetadataUnpackedEventKStream
        .flatMapValues(
            (k, locationMetadata) -> getLocationMetadataUnpackedByAncestry(locationMetadata))
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

    KStream<String, LocationMetadataUnpackedEvent> filter = uuidLocationMetadataUnpackedEventKStream
        .filter((k, locationMetadataUnpackedEvent) ->
            !locationMetadataUnpackedEvent.getPlanTargetType().equals(LocationConstants.STRUCTURE)
        )
        .filter(
            (k, locationMetadataUnpackedEvent) -> locationMetadataUnpackedEvent.getPlanTargetType()
                .equals(locationMetadataUnpackedEvent.getEntityGeoLevel()))
        .mapValues((k, locationMetadataUnpackedEvent) -> getLocationMetadataUnpackedEvent(locationMetadataUnpackedEvent)
        ).selectKey((k, locationMetadataUnpackedEvent) ->
            locationMetadataUnpackedEvent.getPlan() + "_"
                + locationMetadataUnpackedEvent.getEntityId()
        );

    filter.peek((k, v) -> streamLog.debug("filter k:{} v:{}", k, v));

    KTable<String, LocationStructureBusinessStatusAggregate> locationStructureBusinessStatusAggregateTable = filter.groupByKey()
        .aggregate(LocationStructureBusinessStatusAggregate::new,
            (key, locationMetadataUnpackedEvent, aggregate) -> getLocationStructureMetadataAggregate(
                locationMetadataUnpackedEvent, aggregate),
            Materialized.<String, LocationStructureBusinessStatusAggregate, KeyValueStore<Bytes, byte[]>>as(
                    kafkaProperties.getStoreMap().get(KafkaConstants.locationStructureBusinessStatus))
                .withValueSerde(new JsonSerde<>(LocationStructureBusinessStatusAggregate.class))
                .withKeySerde(Serdes.String()));

    locationStructureBusinessStatusAggregateTable.toStream().peek(
        (k, v) -> streamLog.debug("locationStructureBusinessStatusAggregateTable k: {} v: {}", k,
            v));

    KStream<String, LocationStructureBusinessStatusAggregate> stringLocationStructureBusinessStatusAggregateKStream = locationStructureBusinessStatusAggregateTable.toStream()
        .flatMapValues((k, locationStructureBusinessStatusAggregate) ->
            locationRelationshipService.getLocationRelationshipsForLocation(
                    locationStructureBusinessStatusAggregate.getHierarchyIdentifier(),
                    locationStructureBusinessStatusAggregate
                        .getEntityId())
                .getAncestry().stream()
                .map(ancestor -> getLocationStructureBusinessStatusAggregateObject(
                    locationStructureBusinessStatusAggregate,
                    ancestor)).collect(Collectors.toList())
        );

    stringLocationStructureBusinessStatusAggregateKStream.peek(
        (k, v) -> streamLog.debug("stringLocationStructureBusinessStatusAggregateKStream k:{} v:{}",
            k, v));

    KStream<String, LocationStructureBusinessStatusAggregate> stringLocationStructureBusinessStatusAggregateKStream1 = stringLocationStructureBusinessStatusAggregateKStream
        .flatMapValues((k, locationStructureBusinessStatusAggregate) -> {
              List<LocationStructureBusinessStatusAggregate> aggregateList = new ArrayList<>();

              LocationStructureBusinessStatusAggregate locationStructureBusinessStatusAggregate1 = getLocationStructureBusinessStatusAggregateObject(
                  locationStructureBusinessStatusAggregate.getBusinessStatus(),
                  locationStructureBusinessStatusAggregate);

              if (locationStructureBusinessStatusAggregate.getPreviousBusinessStatus() != null) {
                LocationStructureBusinessStatusAggregate locationStructureBusinessStatusAggregate2 = getLocationStructureBusinessStatusAggregateObject(
                    locationStructureBusinessStatusAggregate.getPreviousBusinessStatus(),
                    locationStructureBusinessStatusAggregate);
                aggregateList.add(locationStructureBusinessStatusAggregate2);
              }
              aggregateList.add(locationStructureBusinessStatusAggregate1);
              return aggregateList;
            }
        );

    stringLocationStructureBusinessStatusAggregateKStream1.peek((k, v) -> streamLog.debug(
        "stringLocationStructureBusinessStatusAggregateKStream1 k:{} v:{}", k, v));

    KTable<String, LocationStructureBusinessStatusAggregate> aggregate = stringLocationStructureBusinessStatusAggregateKStream1.selectKey(
            (k, v) -> v.getPlan() + "_" + v.getAncestorNode() + "_" + v.getBusinessStatusKey())
        .groupByKey()
        .aggregate(LocationStructureBusinessStatusAggregate::new,
            (k, v, agg) -> {
              if (agg.getStructureSum() == null) {
                agg.setStructureSum(0L);
              }
              streamLog.trace("setStructureSum1: {} structure Count: {}", agg.getStructureSum(),
                  v.getStructureCounts());
              agg.setStructureSum(agg.getStructureSum() + v.getStructureCounts());
              streamLog.trace("setStructureSum2: {} structure Count: {}", agg.getStructureSum(),
                  v.getStructureCounts());
              return agg;
            },
            Materialized.<String, LocationStructureBusinessStatusAggregate, KeyValueStore<Bytes, byte[]>>as(
                    kafkaProperties.getStoreMap()
                        .get(KafkaConstants.locationStructureHierarchyBusinessStatus))
                .withValueSerde(new JsonSerde<>(LocationStructureBusinessStatusAggregate.class))
                .withKeySerde(Serdes.String()));

    aggregate.toStream().peek((k, v) -> streamLog.debug("aggregate k: {} v: {}", k, v));

    return locationMetadataStream;
  }

  private LocationMetadataUnpackedEvent getLocationMetadataUnpackedEvent(
      LocationMetadataUnpackedEvent locationMetadataUnpackedEvent) {

    Long numberOfChildrenByGeoLevelNameWithinLocationAndHierarchy = locationRelationshipService.getNumberOfChildrenByGeoLevelNameWithinLocationAndHierarchy(
        LocationConstants.STRUCTURE, locationMetadataUnpackedEvent.getEntityId()
        , locationMetadataUnpackedEvent.getHierarchyIdentifier());

    return  LocationMetadataUnpackedEvent.builder()
        .hierarchyIdentifier(locationMetadataUnpackedEvent.getHierarchyIdentifier())
        .entityId(locationMetadataUnpackedEvent.getEntityId())
        .metaDataEvent(locationMetadataUnpackedEvent.getMetaDataEvent())
        .plan(locationMetadataUnpackedEvent.getMetaDataEvent().getTagData().getMeta().getPlanId())
        .planTargetType(locationMetadataUnpackedEvent.getPlanTargetType())
        .entityGeoLevel(locationMetadataUnpackedEvent.getEntityGeoLevel())
        .structureCounts(numberOfChildrenByGeoLevelNameWithinLocationAndHierarchy)
        .build();
  }


  private LocationStructureBusinessStatusAggregate getLocationStructureBusinessStatusAggregateObject(
      String businessStatus,
      LocationStructureBusinessStatusAggregate locationStructureBusinessStatusAggregate) {

    return LocationStructureBusinessStatusAggregate.builder()
        .businessStatus(locationStructureBusinessStatusAggregate.getBusinessStatus())
        .structureCounts(locationStructureBusinessStatusAggregate.getStructureCounts())
        .previousBusinessStatus(locationStructureBusinessStatusAggregate.getPreviousBusinessStatus())
        .plan(locationStructureBusinessStatusAggregate.getPlan())
        .planTargetType(locationStructureBusinessStatusAggregate.getPlanTargetType())
        .entityId(locationStructureBusinessStatusAggregate.getEntityId())
        .entityGeoLevel(locationStructureBusinessStatusAggregate.getEntityGeoLevel())
        .hierarchyIdentifier(locationStructureBusinessStatusAggregate.getHierarchyIdentifier())
        .ancestorNode(locationStructureBusinessStatusAggregate.getAncestorNode())
        .businessStatusKey(businessStatus)
        .build();
  }

  private LocationStructureBusinessStatusAggregate getLocationStructureBusinessStatusAggregateObject(
      LocationStructureBusinessStatusAggregate locationStructureBusinessStatusAggregate,
      UUID ancestor) {

    return LocationStructureBusinessStatusAggregate.builder()
        .businessStatus(locationStructureBusinessStatusAggregate.getBusinessStatus())
        .structureCounts(locationStructureBusinessStatusAggregate.getStructureCounts())
        .previousBusinessStatus(locationStructureBusinessStatusAggregate.getPreviousBusinessStatus())
        .plan(locationStructureBusinessStatusAggregate.getPlan())
        .planTargetType(locationStructureBusinessStatusAggregate.getPlanTargetType())
        .entityId(locationStructureBusinessStatusAggregate.getEntityId())
        .entityGeoLevel(locationStructureBusinessStatusAggregate.getEntityGeoLevel())
        .hierarchyIdentifier(locationStructureBusinessStatusAggregate.getHierarchyIdentifier())
        .ancestorNode(ancestor)
        .build();

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


  private LocationStructureBusinessStatusAggregate getLocationStructureMetadataAggregate(
      LocationMetadataUnpackedEvent locationMetadataUnpackedEvent,
      LocationStructureBusinessStatusAggregate aggregate) {
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
          aggregate.setPreviousBusinessStatus(aggregate.getBusinessStatus());
          aggregate.setBusinessStatus(
              locationMetadataUnpackedEvent.getMetaDataEvent().getTagData().getValue()
                  .getValueString());
          aggregate.setEntityId(locationMetadataUnpackedEvent.getEntityId());

        }
      }
      aggregate.setStructureCounts(locationMetadataUnpackedEvent.getStructureCounts());
      aggregate.setEntityGeoLevel(locationMetadataUnpackedEvent.getEntityGeoLevel());
      aggregate.setHierarchyIdentifier(locationMetadataUnpackedEvent.getHierarchyIdentifier());
      aggregate.setPlanTargetType(locationMetadataUnpackedEvent.getPlanTargetType());
      aggregate.setPlan(locationMetadataUnpackedEvent.getPlan());

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
    return locationRelationShip.getAncestry()
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
            (k, operationalAreaAggregate, aggregate) -> getAggregatedOperationalAreaVisitedCountForMDA(
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

  private OperationalAreaVisitedCount getAggregatedOperationalAreaVisitedCountForMDA(
      OperationalAreaAggregate operationalAreaAggregate, OperationalAreaVisitedCount aggregate) {
    Long sumOfStructures = operationalAreaAggregate.getAggregatedLocationCount().values()
        .stream().reduce(0L, Long::sum);
    Long notVisitedStructures = operationalAreaAggregate.getAggregatedLocationCount().entrySet()
        .stream().filter(entry -> entry.getKey().equals("Not Visited")).map(Entry::getValue)
        .reduce(0L, Long::sum);
    Long notEligibleStructures = operationalAreaAggregate.getAggregatedLocationCount().entrySet()
        .stream().filter(entry -> entry.getKey().equals("Not Eligible")).map(Entry::getValue)
        .reduce(0L, Long::sum);

    Long notSprayedStructures = operationalAreaAggregate.getAggregatedLocationCount().entrySet()
        .stream().filter(entry -> entry.getKey().equals("Not Sprayed")).map(Entry::getValue)
        .reduce(0L, Long::sum);

    Long totalStructuresMDA = sumOfStructures - notEligibleStructures;

    log.trace("operational area: {} -  notVisitedStructures: {} / totalStructures: {} ",
        operationalAreaAggregate.getIdentifier(), notVisitedStructures, totalStructuresMDA);
    boolean operationalAreaIsVisitedMDA = false;

    if (totalStructuresMDA > 0) {

      if ((100 - ((double) notVisitedStructures / (double) totalStructuresMDA * 100))
          >= (double) dashboardProperties.getOperationalAreaVisitedThreshold()) {
        operationalAreaIsVisitedMDA = true;
      }
    }

    Long totalStructuresIRS = sumOfStructures - notEligibleStructures - notSprayedStructures;

    boolean operationalAreaIsVisitedIRS = false;
    boolean operationalAreaIsVisitedEffectivelyIRS = false;

    if (totalStructuresIRS > 0) {

      if ((100 - ((double) notVisitedStructures / (double) totalStructuresIRS * 100))
          >= (double) dashboardProperties.getOperationalAreaVisitedThreshold()) {
        operationalAreaIsVisitedIRS = true;
      }
      if ((100 - ((double) notVisitedStructures / (double) totalStructuresIRS * 100))
          >= (double) dashboardProperties.getOperationalAreaVisitedEffectivelyThreshold()) {
        operationalAreaIsVisitedEffectivelyIRS = true;
      }
    }

    IndividualOperationalAreaCountsByBusinessStatus individualOperationalAreaCountsByBusinessStatus;
    if (aggregate.getOperationalObj().containsKey(operationalAreaAggregate.getIdentifier())) {
      individualOperationalAreaCountsByBusinessStatus = aggregate.getOperationalObj()
          .get(operationalAreaAggregate.getIdentifier());
    } else {
      individualOperationalAreaCountsByBusinessStatus = new IndividualOperationalAreaCountsByBusinessStatus();
    }

    individualOperationalAreaCountsByBusinessStatus.setCounts(
        operationalAreaAggregate.getAggregatedLocationCount());
    individualOperationalAreaCountsByBusinessStatus.setOperationalAreaIsVisitedMDA(
        operationalAreaIsVisitedMDA);
    individualOperationalAreaCountsByBusinessStatus.setOperationalAreaIsVisitedIRS(
        operationalAreaIsVisitedIRS);
    individualOperationalAreaCountsByBusinessStatus.setOperationalAreaIsVisitedEffectivelyIRS(
        operationalAreaIsVisitedEffectivelyIRS);
    aggregate.getOperationalObj().put(operationalAreaAggregate.getIdentifier(),
        individualOperationalAreaCountsByBusinessStatus);

    long countOfVisitedOperationalAreasMDA = aggregate.getOperationalObj().entrySet().stream()
        .filter(entry -> entry.getValue().isOperationalAreaIsVisitedMDA()).count();

    long countOfVisitedOperationalAreasIRS = aggregate.getOperationalObj().entrySet().stream()
        .filter(entry -> entry.getValue().isOperationalAreaIsVisitedIRS()).count();

    long countOfVisitedEffectivelyOperationalAreasIRS = aggregate.getOperationalObj().entrySet()
        .stream()
        .filter(entry -> entry.getValue().isOperationalAreaIsVisitedEffectivelyIRS()).count();

    aggregate.setOperationalAreaVisitedCountMDA(countOfVisitedOperationalAreasMDA);
    aggregate.setOperationalAreaVisitedCountIRS(countOfVisitedOperationalAreasIRS);
    aggregate.setOperationalAreaVisitedEffectivelyIRSCount(
        countOfVisitedEffectivelyOperationalAreasIRS);

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
