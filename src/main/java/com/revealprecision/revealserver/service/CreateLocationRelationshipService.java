package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.constants.KafkaConstants;
import com.revealprecision.revealserver.enums.BulkStatusEnum;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.messaging.message.LocationRelationshipMessage;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationBulk;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.repository.LocationBulkRepository;
import com.revealprecision.revealserver.persistence.repository.LocationHierarchyRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRelationshipRepository;
import com.revealprecision.revealserver.persistence.repository.LocationRepository;
import com.revealprecision.revealserver.props.KafkaProperties;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.geometry.Point;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Profile("Elastic")
@Slf4j
@RequiredArgsConstructor
public class CreateLocationRelationshipService {
  private final LocationRelationshipRepository locationRelationshipRepository;
  private final LocationHierarchyRepository locationHierarchyRepository;
  private final LocationRepository locationRepository;
  @Value(value = "${reveal.elastic.index-name}")
  private String elasticLocationIndexName;
  private final RestHighLevelClient client;
  private final Logger importLog = LoggerFactory.getLogger("location-import-file");
  private final LocationBulkRepository locationBulkRepository;
  private final PublisherService publisherService;
  private final KafkaProperties kafkaProperties;
  private final LocationUtilService locationUtilService;

  @Async("getAsyncExecutor")
  public void createRelationshipForImportedLocation(Location location, int index,
      int locationListSize, LocationBulk bulk) throws IOException {
    List<LocationHierarchy> locationHierarchies = locationHierarchyRepository
        .findLocationHierarchiesByNodeOrderContaining(location.getGeographicLevel().getName());
    for (var locationHierarchy : locationHierarchies) {

      Integer nodePosition =
          locationHierarchy.getNodeOrder().indexOf(location.getGeographicLevel().getName()) - 1;
      if (nodePosition < locationHierarchy.getNodeOrder().size() && nodePosition >= 0) {
        String parentGeographicLevelName = locationHierarchy.getNodeOrder()
            .get(nodePosition);

        String centroid = locationRepository.getCentroid(location.getIdentifier());
        centroid = centroid.substring(6).replace(")", "");
        double x = Double.parseDouble(centroid.split(" ")[0]);
        double y = Double.parseDouble(centroid.split(" ")[1]);
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.filter(QueryBuilders.geoShapeQuery("geometry", new Point(x, y)).relation(
            ShapeRelation.CONTAINS));
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(boolQuery);
        SearchRequest searchRequest = new SearchRequest(elasticLocationIndexName);
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        if (searchResponse.getHits().getHits().length == 0) {
          break;
        } else {
          Map<String, Object> parents = Arrays.stream(
                  searchResponse.getHits().getHits())
              .filter(SearchHit::hasSource).map(SearchHit::getSourceAsMap)
              .map(sourceMap -> new SimpleEntry<String, Object>(
                  String.valueOf(sourceMap.get("level")), sourceMap.get("id")))
              .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> {
                importLog.info("Duplicate Key - Results from Elastic Search - {}",
                    Arrays.stream(searchResponse.getHits().getHits()).map(SearchHit::getSourceAsMap)
                        .map(sourceMap -> sourceMap.get("externalId") + " -> " + sourceMap.get(
                            "level"))
                        .collect(Collectors.joining(","))
                        .concat(" for ")
                        .concat(location.getExternalId().toString())
                        .concat(" -> ")
                        .concat(location.getName())
                        .concat(" - ")
                        .concat(location.getGeographicLevel().getName()));
                return b;
              }));
          List<UUID> parentIds = new ArrayList<>();

          try {
            parentIds = locationHierarchy.getNodeOrder().stream()
                .takeWhile(node -> !node.equals(location.getGeographicLevel().getName()))
                .map(parents::get)
                .map(node -> UUID.fromString((String) node))
                .collect(Collectors.toList());
            Collections.reverse(parentIds);
          } catch (NullPointerException e) {
            e.printStackTrace();
            log.error("Error building ancestry - {}", e.getMessage(), e);
            importLog.debug("Current Ancestry: {}", parents.entrySet().stream()
                .map(entry -> entry.getValue() + " - > " + entry.getKey())
                .collect(Collectors.joining(","))
                .concat(" for ")
                .concat(location.getExternalId().toString())
                .concat(" -> ")
                .concat(location.getName())
                .concat(" - ")
                .concat(location.getGeographicLevel().getName()));
          }

          Optional<SearchHit> immediateParent = Arrays.stream(searchResponse.getHits().getHits())
              .filter(hit -> hit.getSourceAsMap().get("level").equals(parentGeographicLevelName))
              .findFirst();

          LocationRelationshipMessage locationRelationshipMessage = new LocationRelationshipMessage();
          locationRelationshipMessage.setGeoNameLevelNumber(
              locationHierarchy.getNodeOrder().indexOf(
                  location.getGeographicLevel().getName()));
          locationRelationshipMessage.setLocationIdentifier(
              location.getIdentifier());
          locationRelationshipMessage.setGeoName(
              location.getGeographicLevel().getName());

          if (immediateParent.isPresent()) {
            Location parentLoc = Location.builder()
                .identifier(UUID.fromString(immediateParent.get().getId())).build();
            LocationRelationship locationRelationshipToSave = LocationRelationship.builder()
                .parentLocation(parentLoc)
                .location(location)
                .ancestry(parentIds)
                .locationHierarchy(locationHierarchy)
                .build();
            locationRelationshipToSave.setEntityStatus(EntityStatus.ACTIVE);
            locationRelationshipRepository.save(locationRelationshipToSave);
            locationRelationshipMessage.setParentLocationIdentifier(
                locationRelationshipToSave.getParentLocation().getIdentifier());
            locationRelationshipMessage.setAncestry(locationRelationshipToSave.getAncestry());
          }

          locationRelationshipMessage.setLocationName(location.getName());
          locationRelationshipMessage.setLocationHierarchyIdentifier(
              locationHierarchy.getIdentifier());
          publisherService.send(kafkaProperties.getTopicMap().get(KafkaConstants.LOCATIONS_IMPORTED),
              locationRelationshipMessage);


        }
      } else if (nodePosition == -1) {
        locationUtilService.createRelationshipForRoot(location, locationHierarchy);
      }
    }
    if (index == locationListSize - 1) {
      LocationBulk locationBulk = bulk;
      locationBulk.setStatus(BulkStatusEnum.COMPLETE);
      locationBulkRepository.save(locationBulk);

    }
  }
  public void createRootLocationRelationship(Location location,
      LocationHierarchy locationHierarchy) {
    var locationRelationshipToSave = LocationRelationship.builder().location(location)
        .locationHierarchy(locationHierarchy).ancestry(
            Collections.emptyList())
        .build();
    locationRelationshipToSave.setEntityStatus(EntityStatus.ACTIVE);
    locationRelationshipRepository.save(locationRelationshipToSave);
  }


}
