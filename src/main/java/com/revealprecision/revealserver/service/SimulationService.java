package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.DatasetLocationsRequest;
import com.revealprecision.revealserver.api.v1.dto.request.SimulationDatasetRequest;
import com.revealprecision.revealserver.api.v1.dto.response.EntityMetadataResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationPropertyResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.api.v1.dto.response.SimulationDatasetResponse;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Dataset;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.Simulation;
import com.revealprecision.revealserver.persistence.projection.AggregateWithTagProjection;
import com.revealprecision.revealserver.persistence.repository.PlanRepository;
import com.revealprecision.revealserver.persistence.repository.SimulationRepository;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final SimulationRepository simulationRepository;
    private final PlanRepository planRepository;
    private final EntityTagService entityTagService;
    private final LocationService locationService;
    private final RestHighLevelClient client;
    private final LocationHierarchyService locationHierarchyService;
    @Value("${reveal.elastic.index-name}")
    private final String elasticIndex;

    @Transactional
    public Simulation getOrCreateSimulationByPlanId(UUID planId) {
        return simulationRepository.findByPlanIdentifier(planId).orElseGet(() -> {
            Plan plan = planRepository.findById(planId)
                    .orElseThrow(() -> new NotFoundException("Plan not found with ID: " + planId));

            Simulation newSimulation = Simulation.builder()
                    .plan(plan)
                    .datasets(new ArrayList<>())
                    .build();

            return simulationRepository.save(newSimulation);
        });
    }

    public List<LocationResponse> getDatasetDataForLocations(DatasetLocationsRequest request) throws IOException {
        UUID defaultHierarchyId = locationHierarchyService.getDefaultHierarchy().getIdentifier();
        //TODO: check if this ID exists, if not throw exception
        List<String> locationsIds = locationService.getAllLocationDirectChildren(request.getParentLocationId()).stream().map(UUID::toString).collect(Collectors.toList());
        Simulation simulation = simulationRepository.findById(request.getSimulationId()).orElseThrow(() -> new NotFoundException("Simulation not found with ID: " + request.getSimulationId()));
        List <UUID> tagsIds = simulation.getDatasets()
                .stream()
                .filter(dataset -> request.getDatasetsIds().contains(dataset.getIdentifier()))
                .map(dataset -> dataset.getEntityTag().getIdentifier())
                .collect(Collectors.toList());
        List<AggregateWithTagProjection> tags = entityTagService.getValuesForTagAndLocations(tagsIds, locationsIds);
        Map<String, List<EntityMetadataResponse>> metadataMap = tags.stream()
                .collect(Collectors.groupingBy(
                        AggregateWithTagProjection::getLocationIdentifier,
                        Collectors.mapping(
                                tag -> new EntityMetadataResponse(
                                        getRequestedValue(tag),
                                        tag.getTag().getTag(),
                                        tag.getEventType()
                                ),
                                Collectors.toList()
                        )
                ));

        List<LocationResponse> locations;

        if(request.getIncludeGeometry()) {
            SearchRequest searchRequest = new SearchRequest(elasticIndex);
            searchRequest.source(buildLocationWithoutMetadataQuery(locationsIds, defaultHierarchyId));
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            locations =  Arrays.stream(searchResponse.getHits().getHits())
                    .filter(Objects::nonNull).map(hit -> {
                        LocationResponse locationResponse = null;
                        try {
                            locationResponse = LocationResponseFactory.fromSearchHit(hit, null,
                                    defaultHierarchyId.toString());
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }

                        return locationResponse;
                    }).filter(Objects::nonNull).collect(Collectors.toList());
        }  else {
            locations = locationsIds.stream()
                    .map(locationId -> {
                        LocationResponse locationResponse = new LocationResponse();
                        locationResponse.setIdentifier(UUID.fromString(locationId));
                        locationResponse.setProperties(new LocationPropertyResponse());
                        return locationResponse;
                    })
                    .collect(Collectors.toList());
        }
        locations.forEach(locationResponse -> {
            String locationId = locationResponse.getIdentifier().toString();
            List<EntityMetadataResponse> metadata = metadataMap.getOrDefault(locationId, new ArrayList<>());

            if (locationResponse.getProperties() == null) {
                locationResponse.setProperties(new LocationPropertyResponse());
            }
            locationResponse.getProperties().setMetadata(metadata);
        });
        return locations;
    }

    @Transactional
    public SimulationDatasetResponse addDatasetToSimulation(SimulationDatasetRequest request) {
        List<String> locationsIds = locationService.getAllLocationDirectChildren(request.getParentLocationId()).stream().map(UUID::toString).collect(Collectors.toList());
        List<AggregateWithTagProjection> tags = entityTagService.getValuesForTagAndLocations(Collections.singletonList(request.getTagId()), locationsIds);
        Simulation simulation = simulationRepository.findById(request.getSimulationId())
                .orElseThrow(() -> new NotFoundException("Simulation not found with ID: " + request.getSimulationId()));
        AggregateWithTagProjection tagProjection = tags.stream().findFirst().orElseThrow();
        Dataset dataset = Dataset.builder()
                .entityTag(tags.get(0).getTag())
                .hexColor(request.getHexColor())
                .lineWidth(request.getLineWidth())
                .name(tags.get(0).getTag().getTag())
                .build();
        simulation.getDatasets().add(dataset);
        Simulation savedSimulation = simulationRepository.save(simulation);
        Dataset savedDataset = savedSimulation.getDatasets().stream().filter(t -> t.getEntityTag().getIdentifier().equals(tagProjection.getTag().getIdentifier())).findFirst().orElseThrow(() -> new NotFoundException("Could not get dataset of Tag with ID: " + tagProjection.getTag().getIdentifier()));
        Map<String, EntityMetadataResponse> map = tags.stream()
                .collect(Collectors.toMap(
                        AggregateWithTagProjection::getLocationIdentifier,
                        tag -> new EntityMetadataResponse(
                                getRequestedValue(tag),
                                tag.getTag().getTag(),
                                tag.getEventType()
                        )
                ));
        return new SimulationDatasetResponse(savedSimulation.getIdentifier(), tagProjection.getTag().getIdentifier(), savedDataset.getIdentifier(), savedDataset.getName(), savedDataset.getHexColor(), savedDataset.getLineWidth(), map);
    }

    private SearchSourceBuilder buildLocationWithoutMetadataQuery(List<String> locationIds, UUID hierarchyId) {
        var termsQuery = QueryBuilders.termsQuery("id.keyword", locationIds);
        var existsQuery = QueryBuilders.existsQuery("hierarchyDetailsElastic." + hierarchyId);
        var nestedQuery = QueryBuilders.nestedQuery(
                "hierarchyDetailsElastic",
                existsQuery,
                ScoreMode.None
        );
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(termsQuery)
                .must(nestedQuery);
        return new SearchSourceBuilder()
                .fetchSource(null, new String[]{"metadata"})
                .query(boolQuery)
                .size(10000);
    }

    private Double getRequestedValue(AggregateWithTagProjection projection) {
        // Split the tag with suffix into name and aggregation type
        String tagName = projection.getTag().getTag();
        String[] parts = tagName.split("-(?=[^-]+$)");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid tag name format: " + tagName);
        }
        String aggregationType = parts[1];

        switch (aggregationType.toLowerCase()) {
            case "max":
                return projection.getMax();
            case "min":
                return projection.getMin();
            case "average":
                return projection.getAvg();
            case "sum":
                return projection.getSum();
            case "median":
                return projection.getMedian();
            default:
                throw new IllegalArgumentException("Unsupported aggregation type: " + aggregationType);
        }
    }
}