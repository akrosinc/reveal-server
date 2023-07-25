package com.revealprecision.revealserver.messaging.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.messaging.message.GeneratedHierarchyMetadataEvent;
import com.revealprecision.revealserver.persistence.es.EntityMetadataElastic;
import com.revealprecision.revealserver.persistence.es.HierarchyDetailsElastic;
import com.revealprecision.revealserver.persistence.es.LocationElastic;
import com.revealprecision.revealserver.persistence.repository.LocationElasticRepository;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Profile("KafkaMessaging & Simulation & (Listening | location-metadata-update-listener))")
@Slf4j
public class LocationMetadataUpdateListener extends Listener {

  private final RestHighLevelClient client;

  @Value("${reveal.elastic.index-name}")
  String elasticIndex;

  private final LocationElasticRepository locationElasticRepository;


  @KafkaListener(topics = "#{kafkaConfigProperties.topicMap.get('LOCATION_METADATA_UPDATE')}", groupId = "reveal_server_group", containerFactory = "kafkaBatchListenerContainerFactory")
  public void updateLocationMetadata(
      List<GeneratedHierarchyMetadataEvent> generatedHierarchyMetadatas) throws IOException {
    log.debug("Received Message in group foo: {}", generatedHierarchyMetadatas.toString());
    init();

    Map<String, List<GeneratedHierarchyMetadataEvent>> incomingMetadataByLocation = generatedHierarchyMetadatas.stream()
        .collect(Collectors.groupingBy(GeneratedHierarchyMetadataEvent::getLocationIdentifier));

    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    TermsQueryBuilder termsQueryBuilder = QueryBuilders.termsQuery("_id",
        generatedHierarchyMetadatas.stream()
            .map(GeneratedHierarchyMetadataEvent::getLocationIdentifier)
            .collect(Collectors.toList()));

    sourceBuilder.query(termsQueryBuilder);

    SearchRequest searchRequest = new SearchRequest(elasticIndex);
    searchRequest.source(sourceBuilder);
    sourceBuilder.size(10000);

    SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

    ObjectMapper mapper = new ObjectMapper();

    Map<String, LocationElastic> existingLocationElasticMap = Arrays.stream(
            searchResponse.getHits().getHits()).map(hit -> {
          try {
            return mapper.readValue(hit.getSourceAsString(), LocationElastic.class);
          } catch (JsonProcessingException e) {
            e.printStackTrace();
          }
          return null;
        }).filter(Objects::nonNull)
        .collect(Collectors.toMap(LocationElastic::getId, a -> a, (a, b) -> b));

    Map<String, Map<String, Optional<GeneratedHierarchyMetadataEvent>>> incomingLocationDetailsByLocationByHierarchy = generatedHierarchyMetadatas.stream()
        .collect(Collectors.groupingBy(GeneratedHierarchyMetadataEvent::getLocationIdentifier,
            Collectors.groupingBy(generatedHierarchyMetadata -> String.valueOf(
                generatedHierarchyMetadata.getGeneratedHierarchy().getId()), Collectors.reducing(
                (generatedHierarchyMetadata1, generatedHierarchyMetadata2) -> generatedHierarchyMetadata1))));

    Map<String, Map<String, HierarchyDetailsElastic>> updatedHierarchyElasticMapByLocation = existingLocationElasticMap.entrySet()
        .stream().map(existingLocationElasticEntry -> {

          LocationElastic locationElastic = existingLocationElasticEntry.getValue();

          Map<String, HierarchyDetailsElastic> existingHierarchyDetailsElastic = locationElastic.getHierarchyDetailsElastic();

          Map<String, HierarchyDetailsElastic> mergedHierarchyDetailsElastic = new HashMap<>(
              existingHierarchyDetailsElastic);

          Map<String, Optional<GeneratedHierarchyMetadataEvent>> stringOptionalMap = incomingLocationDetailsByLocationByHierarchy.get(
              existingLocationElasticEntry.getKey());

          if (stringOptionalMap != null) {
            Map<String, HierarchyDetailsElastic> incomingHierarchyElasticMap = stringOptionalMap.entrySet()
                .stream().filter(hierarchyMetadataEntry -> {
                  if (!existingHierarchyDetailsElastic.containsKey(
                      hierarchyMetadataEntry.getKey())) {
                    return true;
                  }
                  return false;
                }).filter(hierarchyMetadataEntry -> hierarchyMetadataEntry.getValue().isPresent())
                .map(hierarchyMetadataEntry -> {
                  HierarchyDetailsElastic hierarchyDetailsElastic = new HierarchyDetailsElastic();
                  hierarchyDetailsElastic.setGeographicLevelNumber(0);
                  hierarchyDetailsElastic.setAncestry(
                      hierarchyMetadataEntry.getValue().get().getAncestry());
                  hierarchyDetailsElastic.setParent(
                      hierarchyMetadataEntry.getValue().get().getParent());
                  hierarchyDetailsElastic.setGeographicLevelNumber(
                      hierarchyMetadataEntry.getValue().get().getGeographicLevelNumber());
                  return new SimpleEntry<>(hierarchyMetadataEntry.getKey(),
                      hierarchyDetailsElastic);
                }).collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

            incomingHierarchyElasticMap.forEach(
                (k, v) -> mergedHierarchyDetailsElastic.merge(k, v, (k1, v1) -> v1));

          }
          return new SimpleEntry<>(existingLocationElasticEntry.getKey(),
              mergedHierarchyDetailsElastic);
        }).collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

    Map<String, List<EntityMetadataElastic>> entityMetadataToSave = incomingMetadataByLocation.entrySet()
        .stream().filter(incomingMetadataByLocationEntry -> existingLocationElasticMap.containsKey(
            incomingMetadataByLocationEntry.getKey())).map(
            metadataByHierarchyByLocationEntry -> updateMetadataWithIncomingGeneratedData(
                existingLocationElasticMap, metadataByHierarchyByLocationEntry))
        .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

    existingLocationElasticMap.entrySet().stream().forEach(existingLocationElasticEntry -> {
          existingLocationElasticEntry.getValue()
              .setMetadata(entityMetadataToSave.get(existingLocationElasticEntry.getKey()));
          existingLocationElasticEntry.getValue().setHierarchyDetailsElastic(
              updatedHierarchyElasticMapByLocation.get(existingLocationElasticEntry.getKey()));
          locationElasticRepository.save(existingLocationElasticEntry.getValue());

        }

    );

  }

  private SimpleEntry<String, List<EntityMetadataElastic>> updateMetadataWithIncomingGeneratedData(
      Map<String, LocationElastic> existingLocationElasticMap,
      Entry<String, List<GeneratedHierarchyMetadataEvent>> metadataByHierarchyByLocationEntry) {
    List<EntityMetadataElastic> existingMetadataList = existingLocationElasticMap.get(
        metadataByHierarchyByLocationEntry.getKey()).getMetadata();
    List<GeneratedHierarchyMetadataEvent> incomingMetadataList = metadataByHierarchyByLocationEntry.getValue();

    List<EntityMetadataElastic> updatedExistingMetadataList = existingMetadataList.stream()
        .peek(existingMetadata -> {
          Optional<GeneratedHierarchyMetadataEvent> matchingIncomingMetadataOptional = incomingMetadataList.stream()
              .filter(incomingMetadata ->
                  String.valueOf(incomingMetadata.getGeneratedHierarchy().getId())
                      .equals(existingMetadata.getHierarchyIdentifier())
                      && incomingMetadata.getTag().equals(existingMetadata.getTag())).findFirst();

          matchingIncomingMetadataOptional.ifPresent(
              generatedHierarchyMetadata -> existingMetadata.setValueNumber(
                  generatedHierarchyMetadata.getValue()));

        }).collect(Collectors.toList());

    List<GeneratedHierarchyMetadataEvent> incomingMetadataListNotInExistingList = incomingMetadataList.stream()
        .filter(incomingMetadata -> {

          List<EntityMetadataElastic> existingMetadataMatchingIncomingMetadata = updatedExistingMetadataList.stream()
              .filter(updatedExistingMetadata -> {
                if (updatedExistingMetadata.getTag().equals(incomingMetadata.getTag())
                    && updatedExistingMetadata.getHierarchyIdentifier()
                    .equals(String.valueOf(incomingMetadata.getGeneratedHierarchy().getId()))) {
                  return true;
                }

                return false;
              }).collect(Collectors.toList());

          if (existingMetadataMatchingIncomingMetadata.size() > 0) {
            return false;
          }
          return true;
        }).collect(Collectors.toList());

    List<EntityMetadataElastic> incomingMetadataNotInExistingListTransformed = incomingMetadataListNotInExistingList.stream()
        .map(incomingMetadataNotInExistingList -> {
          EntityMetadataElastic entityMetadataElastic = new EntityMetadataElastic();
          entityMetadataElastic.setTag(incomingMetadataNotInExistingList.getTag());
          entityMetadataElastic.setValueNumber(incomingMetadataNotInExistingList.getValue());
          entityMetadataElastic.setHierarchyIdentifier(
              String.valueOf(incomingMetadataNotInExistingList.getGeneratedHierarchy().getId()));
          entityMetadataElastic.setFieldType(incomingMetadataNotInExistingList.getFieldType());
          return entityMetadataElastic;
        }).collect(Collectors.toList());

    updatedExistingMetadataList.addAll(incomingMetadataNotInExistingListTransformed);

    return new SimpleEntry<>(metadataByHierarchyByLocationEntry.getKey(),
        updatedExistingMetadataList);
  }
}

@Setter
@Getter
@ToString
class LocationDetails {

  private List<String> ancestry;
  private String parent;
  private Integer geographicLevelNumber;
}
