package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.DatasetLocationsRequest;
import com.revealprecision.revealserver.api.v1.dto.request.UpdateDatasetRequest;
import com.revealprecision.revealserver.api.v1.dto.request.SimulationDatasetRequest;
import com.revealprecision.revealserver.api.v1.dto.response.*;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.*;
import com.revealprecision.revealserver.persistence.projection.AggregateWithTagProjection;
import com.revealprecision.revealserver.persistence.projection.LocationDetailsProjection;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final EntityFilterEsService filterEsService;
    private final ObjectMapper objectMapper;

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

    public String filterDatasetsPerAdminLevel(SimulationDatasetRequest request) {
        SimulationRequest saved = filterEsService.saveSimulationDatasetRequest(request);
        return saved.getIdentifier().toString();
    }

    public SimulationResponse getSimulationWithTargetAreas(UUID planId) {
        Simulation s = getOrCreateSimulationByPlanId(planId);
        List<Location> l = locationService.getAllTargetAreasOfPlan(planId, s.getPlan().getPlanTargetType().getGeographicLevel().getName());
        return new SimulationResponse(s.getIdentifier(), s.getDatasets(), l.stream().map(LocationResponseFactory::fromEntityWithPopulation).collect(Collectors.toList()));
    }

    public Simulation updateSimulationDataset(UpdateDatasetRequest request) {
        Simulation simulation = simulationRepository.findById(request.getSimulationId()).orElseThrow(() -> new NotFoundException("Simulation not found with ID: " + request.getSimulationId()));
        Dataset dataset = simulation.getDatasets().stream().filter(d -> d.getIdentifier().equals(request.getDatasetId())).findFirst().orElseThrow(() -> new NotFoundException("Dataset not found with ID: " + request.getDatasetId()));
        dataset.setName(Objects.requireNonNullElse(request.getName(), dataset.getName()));
        dataset.setHexColor(Objects.requireNonNullElse(request.getHexColor(), dataset.getHexColor()));
        dataset.setLineWidth(Objects.requireNonNullElse(request.getLineWidth(), dataset.getLineWidth()));
        dataset.setBorderColor(Objects.requireNonNullElse(request.getBorderColor(), dataset.getBorderColor()));
        return simulationRepository.save(simulation);
    }

    public Simulation deleteSimulationDataset(UpdateDatasetRequest request) {
        Simulation simulation = simulationRepository.findById(request.getSimulationId()).orElseThrow(() -> new NotFoundException("Simulation not found with ID: " + request.getSimulationId()));
        Dataset datasetToRemove = simulation.getDatasets().stream().filter(d -> d.getIdentifier().equals(request.getDatasetId())).findFirst().orElseThrow(() -> new NotFoundException("Dataset not found with ID: " + request.getDatasetId()));
        simulation.getDatasets().remove(datasetToRemove);
        return simulationRepository.save(simulation);

    }

    public SseEmitter getDatasetDataForLocations(String requestId) {
        SimulationRequest simulationRequest = filterEsService.getSimulationRequestById(requestId).orElseThrow(() -> new NotFoundException("x"));
        SimulationDatasetRequest request = simulationRequest.getDatasetRequest();
        UUID defaultHierarchyId = locationHierarchyService.getDefaultHierarchy().getIdentifier();
        //TODO: check if this ID exists, if not throw exception
        Simulation simulation = simulationRepository.findById(request.getSimulationId()).orElseThrow(() -> new NotFoundException("Simulation not found with ID: " + request.getSimulationId()));
        List<LocationDetailsProjection> locationDetailsProjections = locationService.getLocationsWithPropertiesForAdminLevel(request.getParentAdminLevel(), defaultHierarchyId, simulation.getPlan().getIdentifier());
        List<String> locationsIds = locationDetailsProjections.stream().map(LocationDetailsProjection::getLocationId).collect(Collectors.toList());
        List<UUID> tagsIds = simulation.getDatasets()
                .stream()
                .filter(dataset -> simulation.getDatasets().stream().map(Dataset::getIdentifier).collect(Collectors.toList()).contains(dataset.getIdentifier()))
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
                                        tag.getEventType(),
                                        simulation.getDatasets().stream().filter(dataset -> dataset.getEntityTag().getIdentifier().equals(tag.getTag().getIdentifier())).findFirst().get().getIdentifier()
                                ),
                                Collectors.toList()
                        )
                ));

        SseEmitter emitter = new SseEmitter(180000L);
        ExecutorService sseExecutor = Executors.newScheduledThreadPool(2);

        sseExecutor.execute(() -> {
            try {
                final int BATCH_SIZE = 100;

                List<List<String>> batches = getBatches(locationsIds, BATCH_SIZE);

                for (List<String> batch : batches) {
                    SearchSourceBuilder query = buildLocationWithoutMetadataQuery(batch, defaultHierarchyId);
                    List<LocationResponse> results = executeSearch(query, defaultHierarchyId);
                    List<LocationResponse> locationsTransformed = results.stream().peek(locationResponse -> {
                        var locationId = locationResponse != null ? locationResponse.getIdentifier() : null;

                        if (locationId != null) {
                            List<EntityMetadataResponse> metadata = metadataMap.getOrDefault(locationId.toString(), new ArrayList<>());
                            var properties = locationResponse.getProperties();
                            Optional<LocationDetailsProjection> projection = locationDetailsProjections.stream().filter(p -> p.getLocationId().equals(locationId.toString())).findFirst();
                            projection.ifPresent(locationDetailsProjection -> {
                                properties.setChildrenNumber(locationDetailsProjection.getChildrenCount());
                                properties.setParentIdentifier(UUID.fromString(locationDetailsProjection.getParentLocationId()));
                                properties.setId(locationDetailsProjection.getLocationId());
                                properties.setAssigned(locationDetailsProjection.getAssigned());
                                properties.setMetadata(metadata);
                                try {
                                    properties.setPopulation(objectMapper.readValue(locationDetailsProjection.getPopulationData(), PopulationResponseData.class));
                                } catch (JsonProcessingException e) {
                                    properties.setPopulation(null);
                                }

                            });
                            locationResponse.setProperties(properties);
                        }
                    }).collect(Collectors.toList());

                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(locationsTransformed)
                            .reconnectTime(3000L));

                    Thread.sleep(100);
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            } finally {
                sseExecutor.shutdown();
            }
        });

        return emitter;
    }

    public List<LocationResponse> getDatasetDataForLocations(DatasetLocationsRequest request) throws IOException {
        UUID defaultHierarchyId = locationHierarchyService.getDefaultHierarchy().getIdentifier();
        //TODO: check if this ID exists, if not throw exception
        Simulation simulation = simulationRepository.findById(request.getSimulationId()).orElseThrow(() -> new NotFoundException("Simulation not found with ID: " + request.getSimulationId()));
        List<LocationDetailsProjection> locationDetailsProjections = locationService.getAllLocationDirectChildrenWithDetails(request.getParentLocationId(), defaultHierarchyId, simulation.getPlan().getIdentifier());
        List<String> locationsIds = locationDetailsProjections.stream().map(LocationDetailsProjection::getLocationId).collect(Collectors.toList());
        List<UUID> tagsIds = simulation.getDatasets()
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
                                        tag.getEventType(),
                                        simulation.getDatasets().stream().filter(dataset -> dataset.getEntityTag().getIdentifier().equals(tag.getTag().getIdentifier())).findFirst().get().getIdentifier()
                                ),
                                Collectors.toList()
                        )
                ));

        List<LocationResponse> locations;

        if (request.getIncludeGeometry()) {
            SearchRequest searchRequest = new SearchRequest(elasticIndex);
            searchRequest.source(buildLocationWithoutMetadataQuery(locationsIds, defaultHierarchyId));
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            locations = Arrays.stream(searchResponse.getHits().getHits())
                    .filter(Objects::nonNull).map(hit -> {
                        LocationResponse locationResponse = null;
                        try {
                            locationResponse = LocationResponseFactory.fromSearchHit(hit, null,
                                    defaultHierarchyId.toString());
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }

                        var locationId = locationResponse != null ? locationResponse.getIdentifier() : null;
                        if (locationId != null) {
                            var properties = locationResponse.getProperties();
                            Optional<LocationDetailsProjection> projection = locationDetailsProjections.stream().filter(p -> p.getLocationId().equals(locationId.toString())).findFirst();
                            projection.ifPresent(locationDetailsProjection -> {
                                properties.setChildrenNumber(locationDetailsProjection.getChildrenCount());
                                properties.setParentIdentifier(UUID.fromString(locationDetailsProjection.getParentLocationId()));
                                properties.setId(locationDetailsProjection.getLocationId());
                                properties.setAssigned(locationDetailsProjection.getAssigned());
                                try {
                                    properties.setPopulation(objectMapper.readValue(locationDetailsProjection.getPopulationData(), PopulationResponseData.class));
                                } catch (JsonProcessingException e) {
                                    properties.setPopulation(null);
                                }
                            });
                            locationResponse.setProperties(properties);
                        }
                        return locationResponse;
                    }).filter(Objects::nonNull).collect(Collectors.toList());
        } else {
            locations = locationDetailsProjections.stream()
                    .map(projection -> {
                        LocationResponse locationResponse = new LocationResponse();
                        locationResponse.setIdentifier(UUID.fromString(projection.getLocationId()));
                        LocationPropertyResponse properties = new LocationPropertyResponse();
                        properties.setChildrenNumber(projection.getChildrenCount());
                        properties.setParentIdentifier(UUID.fromString(projection.getParentLocationId()));
                        properties.setId(projection.getLocationId());
                        properties.setAssigned(projection.getAssigned());
                        try {
                            properties.setPopulation(objectMapper.readValue(projection.getPopulationData(), PopulationResponseData.class));
                        } catch (JsonProcessingException e) {
                            properties.setPopulation(null);
                        }
                        locationResponse.setProperties(properties);
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
        String DEFAULT_BORDER_COLOR = "#000000";
        Dataset dataset = Dataset.builder()
                .entityTag(tags.get(0).getTag())
                .hexColor(request.getHexColor())
                .lineWidth(request.getLineWidth())
                .borderColor(request.getBorderColor() != null ? request.getBorderColor() : DEFAULT_BORDER_COLOR)
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
                                tag.getEventType(),
                                savedDataset.getIdentifier()
                        )
                ));
        return new SimulationDatasetResponse(savedSimulation.getIdentifier(), tagProjection.getTag().getIdentifier(), savedDataset.getIdentifier(), savedDataset.getName(), savedDataset.getHexColor(), savedDataset.getBorderColor(), savedDataset.getLineWidth(), map);
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

    private List<List<String>> getBatches(List<String> items, int batchSize) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < items.size(); i += batchSize) {
            batches.add(items.subList(i, Math.min(i + batchSize, items.size())));
        }
        return batches;
    }

    private List<LocationResponse> executeSearch(SearchSourceBuilder query, UUID hierarchyId) {
        SearchRequest searchRequest = new SearchRequest(elasticIndex);
        searchRequest.source(query);

        try {
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            return Arrays.stream(response.getHits().getHits())
                    .filter(Objects::nonNull).map(hit -> {
                        LocationResponse locationResponse = null;
                        try {
                            locationResponse = LocationResponseFactory.fromSearchHit(hit, null,
                                    hierarchyId.toString());
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }

                        return locationResponse;
                    }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Error executing Elasticsearch query", e);
        }
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