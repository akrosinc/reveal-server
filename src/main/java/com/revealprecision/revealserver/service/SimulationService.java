package com.revealprecision.revealserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revealprecision.revealserver.api.v1.dto.factory.LocationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.OrganizationResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.DatasetLocationsRequest;
import com.revealprecision.revealserver.api.v1.dto.request.UpdateDatasetRequest;
import com.revealprecision.revealserver.api.v1.dto.request.SimulationDatasetRequest;
import com.revealprecision.revealserver.api.v1.dto.response.*;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.*;
import com.revealprecision.revealserver.persistence.projection.AggregateWithTagProjection;
import com.revealprecision.revealserver.persistence.projection.LocationDetailsProjection;
import com.revealprecision.revealserver.persistence.projection.LocationWithAncestryProjection;
import com.revealprecision.revealserver.persistence.repository.PlanLocationsRepository;
import com.revealprecision.revealserver.persistence.repository.PlanRepository;
import com.revealprecision.revealserver.persistence.repository.SimulationRepository;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
    private final PlanAssignmentService planAssignmentService;
    private final LocationBusinessStatusService locationBusinessStatusService;

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
        List<LocationWithAncestryProjection> l = locationService.getAllTargetAreasOfPlan(planId, s.getPlan().getPlanTargetType().getGeographicLevel().getName());
        List<LocationResponse> locationsResponse = l.stream()
                .map(loc -> LocationResponseFactory.fromEntityWithPopulationAndAncestry(loc.getLocation(), loc.getAncestry().toString(), loc.getNumberOfTeams())).collect(Collectors.toList());
        return new SimulationResponse(s.getIdentifier(), s.getDatasets(), locationsResponse);
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
                    SearchSourceBuilder query = buildLocationWithoutMetadataQuery(batch, defaultHierarchyId, new ArrayList<String>());
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

    // Used only for polygons, structures need to be fetched by bbox
    public List<LocationResponse> getDatasetDataForLocations(DatasetLocationsRequest request) {

        UUID defaultHierarchyId = locationHierarchyService.getDefaultHierarchy().getIdentifier();
        Simulation simulation = simulationRepository.findById(request.getSimulationId())
                .orElseThrow(() -> new NotFoundException("Simulation not found with ID: " + request.getSimulationId()));

        List<LocationDetailsProjection> locationDetailsProjections = locationService.getAllLocationDirectChildrenWithDetails(
                request.getParentLocationId(), defaultHierarchyId, simulation.getPlan().getIdentifier());

        List<String> locationsIds = locationDetailsProjections.stream()
                .map(LocationDetailsProjection::getLocationId)
                .collect(Collectors.toList());

        Map<String, UUID> tagsMap = buildTagsMap(simulation, request.getDatasetsIds());

        Map<String, List<EntityMetadataResponse>> metadataMap = request.getCampaignManagementFeatures()
                ? Collections.emptyMap()
                : fetchMetadata(simulation, request.getDatasetsIds(), locationsIds);

        List<LocationResponse> locations;
        if (request.getIncludeGeometry()) {
            locations = fetchLocationsWithGeometry(defaultHierarchyId, tagsMap, locationsIds);
        } else {
            locations = new ArrayList<>();
        }

        Map<String, LocationResponse> locationsMap = locations.stream()
                .collect(Collectors.toMap(loc -> loc.getIdentifier().toString(), loc -> loc));

        setLocationProperties(locationDetailsProjections).forEach(locationWithoutGeometry -> {
            LocationResponse location = locationsMap.get(locationWithoutGeometry.getIdentifier().toString());
            if (location != null) {
                String locationName = location.getProperties() != null ? location.getProperties().getName() : null;
                location.setProperties(locationWithoutGeometry.getProperties());
                location.getProperties().setName(locationName);
            } else {
                locations.add(locationWithoutGeometry);
            }
        });

        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        if (request.getCampaignManagementFeatures()) {
            tasks.add(CompletableFuture.runAsync(() -> applyPlanAssignments(locations, simulation)));
            tasks.add(CompletableFuture.runAsync(() -> applyBusinessStatus(locations, defaultHierarchyId, simulation)));
        } else {
            tasks.add(CompletableFuture.runAsync(() -> applyMetadataAndPopulationData(locations, metadataMap, locationDetailsProjections)));
            tasks.add(CompletableFuture.runAsync(() -> applyStructureCounts(locations, defaultHierarchyId)));
        }
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();


        return locations;
    }

    @Transactional
    public SimulationDatasetResponse addDatasetToSimulation(SimulationDatasetRequest request) {
        Simulation simulation = simulationRepository.findById(request.getSimulationId())
                .orElseThrow(() -> new NotFoundException("Simulation not found with ID: " + request.getSimulationId()));

        boolean returnLocationData = request.getParentLocationId() != null;
        EntityTag tagForDataset;
        List<AggregateWithTagProjection> tags = Collections.emptyList();
        if (returnLocationData) {
            List<String> locationsIds = locationService.getAllLocationDirectChildren(request.getParentLocationId()).stream().map(UUID::toString).collect(Collectors.toList());
            tags = entityTagService.getValuesForTagAndLocations(
                    Collections.singletonList(request.getTagId()), locationsIds);
            if (tags.isEmpty()) {
                throw new NotFoundException("No tags found for Tag ID: " + request.getTagId());
            }
            tagForDataset = tags.get(0).getTag();
        } else {
            tagForDataset = entityTagService.getEntityTagById(request.getTagId());
        }

        Dataset dataset = createDataset(request, tagForDataset);
        simulation.getDatasets().add(dataset);
        Simulation savedSimulation = simulationRepository.save(simulation);

        Dataset savedDataset = findSavedDataset(savedSimulation, tagForDataset.getIdentifier());

        return new SimulationDatasetResponse(
                savedSimulation.getIdentifier(),
                request.getTagId(),
                savedDataset.getIdentifier(),
                savedDataset.getName(),
                savedDataset.getHexColor(),
                savedDataset.getBorderColor(),
                savedDataset.getLineWidth(),
                returnLocationData ? buildMetadataMap(tags, savedDataset) : Collections.emptyMap()
        );
    }

    private Dataset createDataset(SimulationDatasetRequest request, EntityTag tag) {
        final String DEFAULT_BORDER_COLOR = "#000000";
        return Dataset.builder()
                .entityTag(tag)
                .hexColor(request.getHexColor())
                .lineWidth(request.getLineWidth())
                .borderColor(request.getBorderColor() != null ? request.getBorderColor() : DEFAULT_BORDER_COLOR)
                .name(tag.getTag())
                .build();
    }

    private Dataset findSavedDataset(Simulation savedSimulation, UUID tagId) {
        return savedSimulation.getDatasets().stream()
                .filter(dataset -> dataset.getEntityTag().getIdentifier().equals(tagId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Could not retrieve dataset for Tag ID: " + tagId));
    }

    private Map<String, EntityMetadataResponse> buildMetadataMap(List<AggregateWithTagProjection> tags, Dataset savedDataset) {
        return tags.stream()
                .collect(Collectors.toMap(
                        AggregateWithTagProjection::getLocationIdentifier,
                        tag -> new EntityMetadataResponse(
                                getRequestedValue(tag),
                                tag.getTag().getTag(),
                                tag.getEventType(),
                                savedDataset.getIdentifier()
                        )
                ));
    }

    private SearchSourceBuilder buildLocationWithoutMetadataQuery(List<String> locationIds, UUID hierarchyId, Collection<String> tagsNames) {
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

        String collect = tagsNames.stream()
                .map(tag -> "n.add('" + tag + "');")
                .collect(Collectors.joining(""));
        Script inline = new Script(ScriptType.INLINE, "painless",
                "List a = params['_source']['metadata']; List n = new ArrayList(); " + collect
                        + "  return a.stream().filter(val->n.contains(val.tag) && val.hierarchyIdentifier.equals('"
                        + hierarchyId + "')).collect(Collectors.toList());",
                new HashMap<>());

        return new SearchSourceBuilder()
                .fetchSource(null, new String[]{"metadata"})
                .query(boolQuery)
                .scriptField("meta", inline)
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

    private Map<String, UUID> buildTagsMap(Simulation simulation, List<UUID> requestedDatasetIds) {
        Set<UUID> requestedIdsSet = new HashSet<>(requestedDatasetIds);
        return simulation.getDatasets()
                .stream()
                .filter(dataset -> requestedIdsSet.contains(dataset.getIdentifier()))
                .collect(Collectors.toMap(
                        dataset -> dataset.getEntityTag().getTag(),
                        Dataset::getIdentifier
                ));
    }

    private Map<String, List<EntityMetadataResponse>> fetchMetadata(
            Simulation simulation, List<UUID> requestedDatasetIds, List<String> locationsIds) {

        List<UUID> tagsIds = simulation.getDatasets()
                .stream()
                .filter(dataset -> requestedDatasetIds.contains(dataset.getIdentifier()))
                .map(dataset -> dataset.getEntityTag().getIdentifier())
                .collect(Collectors.toList());

        List<AggregateWithTagProjection> tags = entityTagService.getValuesForTagAndLocations(tagsIds, locationsIds);

        return tags.stream().collect(Collectors.groupingBy(
                AggregateWithTagProjection::getLocationIdentifier,
                Collectors.mapping(tag -> new EntityMetadataResponse(
                        getRequestedValue(tag),
                        tag.getTag().getTag(),
                        tag.getEventType(),
                        simulation.getDatasets().stream()
                                .filter(dataset -> dataset.getEntityTag().getIdentifier().equals(tag.getTag().getIdentifier()))
                                .findFirst()
                                .get()
                                .getIdentifier()
                ), Collectors.toList())
        ));
    }

    private List<LocationResponse> fetchLocationsWithGeometry(
            UUID defaultHierarchyId, Map<String, UUID> tagsMap, List<String> locationsIds) {

        SearchRequest searchRequest = new SearchRequest(elasticIndex);
        searchRequest.source(buildLocationWithoutMetadataQuery(locationsIds, defaultHierarchyId, tagsMap.keySet()));

        try {
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            return Arrays.stream(searchResponse.getHits().getHits())
                    .filter(Objects::nonNull).map(hit -> {
                        try {
                            return LocationResponseFactory.fromSearchHit(hit, null,
                                    defaultHierarchyId.toString());
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<LocationResponse> setLocationProperties(List<LocationDetailsProjection> locationDetailsProjections) {
        return locationDetailsProjections.stream().map(projection -> {
            LocationResponse locationResponse = new LocationResponse();
            locationResponse.setIdentifier(UUID.fromString(projection.getLocationId()));

            LocationPropertyResponse properties = new LocationPropertyResponse();
            properties.setChildrenNumber(projection.getChildrenCount());
            properties.setParentIdentifier(UUID.fromString(projection.getParentLocationId()));
            properties.setId(projection.getLocationId());
            properties.setGeographicLevel(projection.getGeographicLevelName());
            properties.setAssigned(projection.getAssigned());
            locationResponse.setProperties(properties);

            List<String> ancestry = projection.getAncestry();
            locationResponse.setAncestry(ancestry == null || ancestry.isEmpty() || ancestry.get(0) == null || ancestry.get(0).isBlank()
                    ? Collections.emptyList()
                    : Arrays.stream(ancestry.get(0).split(",")).collect(Collectors.toList()));

            return locationResponse;
        }).collect(Collectors.toList());
    }

    private void applyPlanAssignments(List<LocationResponse> locations, Simulation simulation) {
        List<PlanAssignment> planAssignments = planAssignmentService.getPlanAssignmentsByPlanIdentifier(simulation.getPlan().getIdentifier());
        Map<UUID, List<PlanAssignment>> planAssignmentMap = planAssignments.stream()
                .collect(Collectors.groupingBy(
                        planAssignment -> planAssignment.getPlanLocations().getLocation().getIdentifier()));

        locations.forEach(loc -> {
            List<PlanAssignment> assignments = planAssignmentMap.get(loc.getIdentifier());
            List<OrganizationResponse> teams = (assignments != null)
                    ? assignments.stream()
                    .map(el -> OrganizationResponseFactory.fromEntityIdAndName(el.getOrganization()))
                    .collect(Collectors.toList())
                    : Collections.emptyList();
            loc.setTeams(teams);
        });
    }

    private void applyBusinessStatus(List<LocationResponse> locations, UUID defaultHierarchyId, Simulation simulation) {
        locations.forEach(loc -> {
            if (Objects.equals(loc.getProperties().getGeographicLevel(), "structure")) {
                String taskStatus = locationBusinessStatusService.findLocationBusinessState(defaultHierarchyId,
                        loc.getIdentifier(), simulation.getPlan().getIdentifier());
                if (taskStatus != null) {
                    loc.getProperties().setBusinessStatus(taskStatus);
                }
            }
        });
    }

    private void applyMetadataAndPopulationData(List<LocationResponse> locations, Map<String, List<EntityMetadataResponse>> metadataMap, List<LocationDetailsProjection> projections) {
        locations.forEach(loc -> {
            String locationId = loc.getIdentifier().toString();
            loc.getProperties().setMetadata(metadataMap.getOrDefault(locationId, new ArrayList<>()));

            if (loc.getProperties().getPopulation() == null) {
                try {
                    Optional<LocationDetailsProjection> projection = projections.stream().filter(p -> p.getLocationId().equals(loc.getIdentifier().toString())).findFirst();
                    if (projection.isPresent()) {
                        loc.getProperties().setPopulation(objectMapper.readValue(projection.get().getPopulationData(), PopulationResponseData.class));
                    }
                } catch (JsonProcessingException e) {
                    loc.getProperties().setPopulation(null);
                }
            }
        });
    }

    private void applyStructureCounts(List<LocationResponse> locations, UUID defaultHierarchyId) {
        locations.forEach(loc -> {
            try {
                loc.getProperties().setNumberOfStructures(countMatchingLocations(defaultHierarchyId, loc.getIdentifier()));
            } catch (IOException e) {
                loc.getProperties().setNumberOfStructures(0L);
            }
        });
    }

    public long countMatchingLocations(UUID hierarchyId, UUID parentLocationId) throws IOException {
        SearchRequest searchRequest = new SearchRequest(elasticIndex);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(0);
        sourceBuilder.trackTotalHits(true);
        TermQueryBuilder levelQuery = QueryBuilders.termQuery("level", "structure");

        String dynamicField = "hierarchyDetailsElastic." + hierarchyId.toString() + ".ancestry.keyword";
        TermQueryBuilder nestedTermQuery = QueryBuilders.termQuery(dynamicField, parentLocationId.toString());

        NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("hierarchyDetailsElastic", nestedTermQuery, ScoreMode.Avg);

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(levelQuery)
                .must(nestedQuery);

        sourceBuilder.query(boolQuery);

        searchRequest.source(sourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        return Objects.requireNonNull(searchResponse.getHits().getTotalHits()).value;
    }
}