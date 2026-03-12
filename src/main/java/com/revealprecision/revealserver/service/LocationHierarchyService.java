package com.revealprecision.revealserver.service;

import static java.util.stream.Collectors.joining;

import com.revealprecision.revealserver.api.v1.dto.request.LocationHierarchyRequest;
import com.revealprecision.revealserver.api.v1.dto.response.GeoTreeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationPropertyResponse;
import com.revealprecision.revealserver.enums.BulkStatusEnum;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.NotImplementedException;
import com.revealprecision.revealserver.exceptions.constant.Error;
import com.revealprecision.revealserver.persistence.domain.Location;
import com.revealprecision.revealserver.persistence.domain.LocationBulk;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import com.revealprecision.revealserver.persistence.projection.LocationChildrenCountProjection;
import com.revealprecision.revealserver.persistence.projection.LocationRelationshipProjection;
import com.revealprecision.revealserver.persistence.repository.LocationHierarchyRepository;
import com.revealprecision.revealserver.util.AppConstants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationHierarchyService {

    private final LocationHierarchyRepository locationHierarchyRepository;
    private final LocationRelationshipService locationRelationshipService;
    private final GeographicLevelService geographicLevelService;
    private final LocationBulkService locationBulkService;
    private final RestHighLevelClient client;
    private final CreateLocationRelationshipService createLocationRelationshipService;

    @Value("${reveal.elastic.index-name}")
    private final String elasticIndex;


    public LocationHierarchy createLocationHierarchy(
            LocationHierarchyRequest locationHierarchyRequest) {
//        enforceOneHierarchyPerInstance();

        LocationHierarchy baseHierarchy =  getBaseHierarchy();
        locationHierarchyRequest.getNodeOrder().addAll(0, baseHierarchy.getNodeOrder());

        geographicLevelService.validateGeographyLevels(locationHierarchyRequest.getNodeOrder());
        validateLocationHierarchy(locationHierarchyRequest);

        var locationHierarchyToSave = LocationHierarchy.builder()
                .nodeOrder(locationHierarchyRequest.getNodeOrder()).name(locationHierarchyRequest.getName())
                .build();
        locationHierarchyToSave.setEntityStatus(EntityStatus.ACTIVE);
        return locationHierarchyRepository.save(locationHierarchyToSave);
    }

    private void enforceOneHierarchyPerInstance() {
        if (locationHierarchyRepository.activeHierarchyCount() > 0) {
            throw new NotImplementedException(Error.ONE_HIERARCHY_SUPPORT);
        }
    }

    private void validateLocationHierarchy(LocationHierarchyRequest locationHierarchyRequest) {
        List<LocationHierarchy> existingHierarchy = findByNodeOrder(
                locationHierarchyRequest.getNodeOrder());
        if (existingHierarchy != null && !existingHierarchy.isEmpty()) {
            throw new ConflictException(
                    String.format(Error.NON_UNIQUE, LocationHierarchy.Fields.nodeOrder,
                            locationHierarchyRequest.getNodeOrder()));
        }
    }

    public Page<LocationHierarchy> getLocationHierarchies(Pageable pageable) {
        return locationHierarchyRepository.findAll(pageable);
    }

    public Set<LocationHierarchy> getLocationHierarchiesIn(Set<UUID> locationHierarchyIdentifiers) {
        return locationHierarchyRepository.findLocationHierarchiesByIdentifierIn(
                locationHierarchyIdentifiers);
    }

    public List<LocationHierarchy> findByNodeOrder(List<String> nodeOrder) {
        return locationHierarchyRepository
                .findByNodeOrderArray(nodeOrder.stream().collect(joining(",", "{", "}")));
    }

    public List<LocationHierarchy> getAll() {
        return locationHierarchyRepository
                .findAll();
    }

    public void deleteLocationHierarchyAndAssociatedLocationRelationships(UUID identifier) {
        LocationHierarchy locationHierarchy = findByIdentifier(identifier);
        locationRelationshipService.deleteLocationRelationshipsForHierarchy(locationHierarchy);
        deleteLocationHierarchy(locationHierarchy);
    }

    private void deleteLocationHierarchy(LocationHierarchy locationHierarchy) {
        locationHierarchyRepository.delete(locationHierarchy);
    }

    public LocationHierarchy findByIdentifier(UUID identifier) {
        return locationHierarchyRepository.findById(identifier).orElseThrow(
                () -> new NotFoundException(Pair.of(LocationHierarchy.Fields.identifier, identifier),
                        LocationHierarchy.class));
    }
    public UUID findLocationHierarchyIdentifierByIdentifier(UUID identifier) {
        return locationHierarchyRepository.findLocationHierarchyIdentifierByIdentifier(identifier).orElseThrow(
            () -> new NotFoundException(Pair.of(LocationHierarchy.Fields.identifier, identifier),
                LocationHierarchy.class));
    }

    public LocationHierarchy findLocationHierarchyByIdentifier(UUID identifier) {
        return locationHierarchyRepository.findLocationHierarchyObjByIdentifier(identifier).orElseThrow(
            () -> new NotFoundException(Pair.of(LocationHierarchy.Fields.identifier, identifier),
                LocationHierarchy.class));
    }


    public List<String> findNodeOrderByIdentifier(UUID identifier) {
        return Arrays.asList(
                locationHierarchyRepository.findNodeOrderByIdentifier(identifier).split(","));
    }

    public UUID findNativeByName(String hierarchyName) {
        return locationHierarchyRepository.findLocationHierarchyByName(hierarchyName);
    }

    public LocationHierarchy getDefaultHierarchy() {
        return locationHierarchyRepository.findByName(AppConstants.DEFAULT_KEYWORD).orElseThrow(() -> new NotFoundException("Default hierarchy not found"));
    }

    public UUID findNativeById(UUID hierarchyIdentifier) {
        return locationHierarchyRepository.findLocationHierarchyByIdentifier(hierarchyIdentifier);
    }

    public List<GeoTreeResponse> getGeoTreeFromLocationHierarchy(
            LocationHierarchy locationHierarchy, boolean excludeStructures) {
        List<LocationRelationship> locationRelationship;
        if (excludeStructures) {
            locationRelationship = getLocationRelationshipsWithoutStructuresForLocationHierarchy(locationHierarchy);
        } else {
            locationRelationship = getLocationRelationshipsForLocationHierarchy(
                    locationHierarchy);
        }
        List<GeoTreeResponse> geoTreeResponses = locationRelationship.stream()
                .map(lr -> GeoTreeResponse.builder()
                        .identifier(lr.getLocation().getIdentifier())
                        .properties(LocationPropertyResponse.builder()
                                .parentIdentifier((lr.getParentLocation() == null) ? UUID.fromString(
                                        "00000000-0000-0000-0000-000000000000")
                                        : lr.getParentLocation().getIdentifier())
                                .name(lr.getLocation().getName())
                                .geographicLevel(lr.getLocation().getGeographicLevel().getName())
                                .build())
                        .build()).collect(Collectors.toList());
        Map<UUID, List<GeoTreeResponse>> geoTreeHierarchy = geoTreeResponses.stream()
                .collect(Collectors.groupingBy(lr -> lr.getProperties().getParentIdentifier(),
                        Collectors.mapping(lr -> lr, Collectors.toList())));

        geoTreeResponses.forEach(gt -> gt.setChildren(
                geoTreeHierarchy.get(gt.getIdentifier()) == null ? new ArrayList<>()
                        : geoTreeHierarchy.get(gt.getIdentifier())));
        return geoTreeHierarchy.get(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    public List<GeoTreeResponse> getGeoTreeWithoutStructuresES(UUID locationHierarchyId) throws IOException {
        List<Map<String, Object>> documents = getNonStructureLocations(locationHierarchyId);

        List<GeoTreeResponse> geoTreeResponses = documents.stream().map(doc -> {
                    String level = (String) doc.get("level");
                    String name = (String) doc.get("name");
                    String id = (String) doc.get("id");
                    String parent = "00000000-0000-0000-0000-000000000000";
                    Map<String, Object> hierarchyDetails = (Map<String, Object>) doc.get("hierarchyDetailsElastic");
                    if (hierarchyDetails != null) {
                        Map<String, Object> dynamicDetails = (Map<String, Object>) hierarchyDetails.get(locationHierarchyId.toString());
                        if (dynamicDetails != null) {
                            parent = (String) dynamicDetails.get("parent");
                        }
                    }
                    if (parent == null ){
                        parent = "00000000-0000-0000-0000-000000000000";
                    }

                    if (id != null && level != null && name != null && parent != null) {
                        return GeoTreeResponse.builder()
                                .identifier(UUID.fromString(id))
                                .properties(
                                        LocationPropertyResponse.builder()
                                                .parentIdentifier(UUID.fromString(parent))
                                                .name(name)
                                                .geographicLevel(level)
                                                .build()
                                ).build();
                    } else return null;
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<UUID, List<GeoTreeResponse>> geoTreeHierarchy = geoTreeResponses.stream()
                .collect(Collectors.groupingBy(lr -> lr.getProperties().getParentIdentifier(),
                        Collectors.mapping(lr -> lr, Collectors.toList())));

        geoTreeResponses.forEach(gt -> gt.setChildren(
                geoTreeHierarchy.get(gt.getIdentifier()) == null ? new ArrayList<>()
                        : geoTreeHierarchy.get(gt.getIdentifier())));
        return geoTreeHierarchy.get(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    public List<GeoTreeResponse> getGeoTreeFromLocationHierarchyWithoutStructure(
            LocationHierarchy locationHierarchy, List<String> notLike) {
        List<LocationRelationshipProjection> locationRelationship =
                notLike != null ? locationRelationshipService.getLocationRelationshipsNotLike(
                        locationHierarchy, notLike)
                        : locationRelationshipService.getLocationRelationshipsWithoutStructure(
                        locationHierarchy);
        Map<String, Long> childrenCount = locationRelationshipService.getLocationChildrenCount(
                        locationHierarchy.getIdentifier())
                .stream().filter(loc -> loc.getParentIdentifier() != null)
                .collect(Collectors.toMap(LocationChildrenCountProjection::getParentIdentifier,
                        LocationChildrenCountProjection::getChildrenCount));

        List<GeoTreeResponse> geoTreeResponses = locationRelationship.stream()
                .map(lr -> GeoTreeResponse.builder()
                        .identifier(UUID.fromString(lr.getLocationIdentifier()))
                        .properties(LocationPropertyResponse.builder()
                                .parentIdentifier((lr.getParentIdentifier() == null) ? UUID.fromString(
                                        "00000000-0000-0000-0000-000000000000")
                                        : UUID.fromString(lr.getParentIdentifier()))
                                .name(lr.getLocationName())
                                .geographicLevel(lr.getGeographicLevelName())
                                .childrenNumber(
                                        childrenCount.containsKey(lr.getLocationIdentifier()) ? childrenCount.get(
                                                lr.getLocationIdentifier()) : 0)
                                .build())
                        .build()).collect(Collectors.toList());
        Map<UUID, List<GeoTreeResponse>> geoTreeHierarchy = geoTreeResponses.stream()
                .collect(Collectors.groupingBy(lr -> lr.getProperties().getParentIdentifier(),
                        Collectors.mapping(lr -> lr, Collectors.toList())));

        geoTreeResponses.forEach(gt -> gt.setChildren(
                geoTreeHierarchy.get(gt.getIdentifier()) == null ? new ArrayList<>()
                        : geoTreeHierarchy.get(gt.getIdentifier())));
        return geoTreeHierarchy.get(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    public List<LocationRelationship> getLocationRelationshipsForLocationHierarchy(
            LocationHierarchy locationHierarchy) {
        return locationRelationshipService
                .getLocationRelationshipsForLocationHierarchy(locationHierarchy);
    }

    public List<LocationRelationship> getLocationRelationshipsWithoutStructuresForLocationHierarchy(
            LocationHierarchy locationHierarchy) {
        return locationRelationshipService
                .getLocationRelationshipsWithoutStructuresForLocationHierarchy(locationHierarchy);
    }

    public LocationHierarchy getActiveLocationHierarchy() {
        //Assumption: current support of 1 hierarchy per instance
        LocationHierarchy locationHierarchy = null;
        List<LocationHierarchy> hierarchies = locationHierarchyRepository.findAll();
        if (!hierarchies.isEmpty()) {
            locationHierarchy = hierarchies.get(0);
        }
        return locationHierarchy;
    }

    private List<Map<String, Object>> getNonStructureLocations(UUID hierarchyId) throws IOException {
        final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
        SearchRequest searchRequest = new SearchRequest(elasticIndex);
        searchRequest.scroll(scroll);

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .mustNot(QueryBuilders.termQuery("level", "structure"));
        sourceBuilder.query(boolQuery);

        String parentField  = "hierarchyDetailsElastic." + hierarchyId + ".parent";
        String[] includeFields = new String[]{
                "id",
                "name",
                "level",
                parentField
        };
        sourceBuilder.fetchSource(includeFields, null);
        sourceBuilder.size(1000);
        searchRequest.source(sourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        String scrollId = searchResponse.getScrollId();
        SearchHit[] searchHits = searchResponse.getHits().getHits();

        List<Map<String, Object>> allResults = new ArrayList<>();

        while (searchHits != null && searchHits.length > 0) {
            for (SearchHit hit : searchHits) {
                allResults.add(hit.getSourceAsMap());
            }
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(scroll);
            searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
            scrollId = searchResponse.getScrollId();
            searchHits = searchResponse.getHits().getHits();
        }

        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scrollId);
        client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);

        return allResults;
    }

    public LocationHierarchy createBaseLocationHierarchy(LocationHierarchyRequest locationHierarchyRequest) {
        enforceOneBaseHierarchy();
        geographicLevelService.validateGeographyLevels(locationHierarchyRequest.getNodeOrder());
        validateLocationHierarchy(locationHierarchyRequest);

        var locationHierarchyToSave = LocationHierarchy.builder()
            .nodeOrder(locationHierarchyRequest.getNodeOrder()).name(locationHierarchyRequest.getName())
            .baseHierarchy(true)
            .build();
        locationHierarchyToSave.setEntityStatus(EntityStatus.ACTIVE);
        return locationHierarchyRepository.save(locationHierarchyToSave);
    }

    private void enforceOneBaseHierarchy(){
        if (locationHierarchyRepository.activeBaseHierarchyCount() > 0) {
            throw new ConflictException(Error.ONE_BASE_HIERARCHY_SUPPORT);
        }
    }

    private LocationHierarchy getBaseHierarchy(){
        return locationHierarchyRepository.findByName(AppConstants.DEFAULT_KEYWORD).
            orElseThrow(() -> new NotFoundException("Default hierarchy not found"));
    }

    public void activateLocationHierarchy(UUID identifier) {

       List<LocationBulk>  locationBulks = locationBulkService.getUnCompletedLocationBulk();

       LocationHierarchy locationHierarchy = findByIdentifier(identifier);

        for(LocationBulk locationBulk : locationBulks){

            List<Location> addedLocations = locationBulkService.getAllCreatedInBulk(
                locationBulk.getIdentifier());
            log.info("addLocations size: {}", addedLocations.size());
            int index = 0;
            for (Location location : addedLocations) {
                try {
                    createLocationRelationshipService.createRelationshipForImportedLocationAndHierarchy(location, index,
                        addedLocations.size(), locationBulk, locationHierarchy);
                } catch (IOException e) {
                    log.error("Error creating relationship for location {}", location.getIdentifier(),e);
                }
                index++;
            }
            if (addedLocations.isEmpty()) {
                locationBulk.setStatus(BulkStatusEnum.EMPTY);
                locationBulkService.update(locationBulk);
            }
        }
    }
}
