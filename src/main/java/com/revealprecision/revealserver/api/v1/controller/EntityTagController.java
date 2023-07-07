package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.csv.LocationCSVRecord;
import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.LookupEntityTagResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.DataFilterRequest;
import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;
import com.revealprecision.revealserver.api.v1.dto.request.SaveHierarchyRequest;
import com.revealprecision.revealserver.api.v1.dto.request.UpdateEntityTagRequest;
import com.revealprecision.revealserver.api.v1.dto.response.AggregateHelper;
import com.revealprecision.revealserver.api.v1.dto.response.EntityMetadataResponse;
import com.revealprecision.revealserver.api.v1.dto.response.EntityTagResponse;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponse;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponseContainer;
import com.revealprecision.revealserver.api.v1.dto.response.LookupEntityTypeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PersonMainData;
import com.revealprecision.revealserver.api.v1.dto.response.SaveHierarchyResponse;
import com.revealprecision.revealserver.api.v1.dto.response.SimulationCountResponse;
import com.revealprecision.revealserver.api.v1.facade.factory.LocationCSVRecordFactory;
import com.revealprecision.revealserver.persistence.domain.LocationHierarchy;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import com.revealprecision.revealserver.persistence.domain.SimulationRequest;
import com.revealprecision.revealserver.service.EntityFilterEsService;
import com.revealprecision.revealserver.service.EntityTagService;
import com.revealprecision.revealserver.service.EventAggregationService;
import com.revealprecision.revealserver.service.LocationHierarchyService;
import com.revealprecision.revealserver.service.LookupEntityTypeService;
import io.swagger.v3.oas.annotations.Operation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.csveed.bean.BeanInstructions;
import org.csveed.bean.BeanInstructionsImpl;
import org.csveed.row.RowWriter;
import org.csveed.row.RowWriterImpl;
import org.elasticsearch.search.SearchHit;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/entityTag")
@Profile("Elastic")
public class EntityTagController {

  private final EntityTagService entityTagService;
  private final EntityFilterEsService entityFilterService;
  private final LookupEntityTypeService lookupEntityTypeService;
  private final EventAggregationService eventAggregationService;
  private final LocationHierarchyService locationHierarchyService;

  @Operation(summary = "Create Tag", description = "Create Tag", tags = {"Entity Tags"})
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<EntityTagResponse> createTag(
      @Valid @RequestBody EntityTagRequest entityTagRequest) {
    entityTagRequest.setAddToMetadata(true); //TODO set this value on the frontend and remove this
    return ResponseEntity.status(HttpStatus.CREATED).body(EntityTagResponseFactory.fromEntity(
        entityTagService.createEntityTag(entityTagRequest, true)));
  }


  @Operation(summary = "Get All Entity Tags", description = "Get All Entity Tags", tags = {
      "Entity Tags"})
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Page<EntityTagResponse>> getAll(Pageable pageable,
      @RequestParam(name = "filter", defaultValue = "all") String filter,
      @RequestParam(name = "search", defaultValue = "", required = false) String search) {
    switch (filter) {
      case "global":
        return ResponseEntity.status(HttpStatus.OK).body(EntityTagResponseFactory.fromEntityPage(
            entityTagService.getAllPagedGlobalEntityTags(pageable, search), pageable));
      case "importable":
        return ResponseEntity.status(HttpStatus.OK).body(EntityTagResponseFactory.fromEntityPage(
            entityTagService.getAllPagedGlobalNonAggregateEntityTags(pageable, search), pageable));
      case "all":
      default:
        return ResponseEntity.status(HttpStatus.OK).body(EntityTagResponseFactory.fromEntityPage(
            entityTagService.getAllPagedEntityTags(pageable, search), pageable));
    }
  }


  @GetMapping("/{entityTypeIdentifier}")
  public ResponseEntity<List<EntityTagResponse>> getTagsByEntityType(
      @PathVariable UUID entityTypeIdentifier,
      @RequestParam(name = "filter", defaultValue = "all") String filter) {

    LookupEntityType lookupEntityType = lookupEntityTypeService.getLookUpEntityTypeById(
        entityTypeIdentifier);

    List<EntityTagResponse> coreFields = lookupEntityType.getCoreFields().stream()
        .map(EntityTagResponseFactory::fromCoreField).collect(Collectors.toList());

    switch (filter) {
      case "global":
        coreFields.addAll(entityTagService.getAllGlobalEntityTagsByLookupEntityTypeIdentifier(
                entityTypeIdentifier).stream().map(EntityTagResponseFactory::fromEntity)
            .collect(Collectors.toList()));
        break;

      case "importable":
        coreFields.addAll(
            entityTagService.getAllGlobalNonAggregateEntityTagsByLookupEntityTypeIdentifier(
                    entityTypeIdentifier).stream().map(EntityTagResponseFactory::fromEntity)
                .collect(Collectors.toList()));
        break;
      case "all":
      default:
        coreFields.addAll(
            entityTagService.getEntityTagsByLookupEntityTypeIdentifier(entityTypeIdentifier)
                .stream().map(EntityTagResponseFactory::fromEntity).collect(Collectors.toList()));
        break;
    }

    return ResponseEntity.status(HttpStatus.OK).body(coreFields);
  }

  @GetMapping("/eventBasedTags/{entityTypeIdentifier}")
  public ResponseEntity<List<EntityTagResponse>> getEventBasedTags(
      @PathVariable UUID entityTypeIdentifier) {

    return ResponseEntity.status(HttpStatus.OK)
        .body(eventAggregationService.getEventBasedTags(entityTypeIdentifier));
  }

  @GetMapping("/entityType")
  public ResponseEntity<List<LookupEntityTypeResponse>> getEntityTypes() {
    return ResponseEntity.status(HttpStatus.OK).body(
        lookupEntityTypeService.getAllLookUpEntityTypes().stream()
            .map(LookupEntityTagResponseFactory::fromEntity).collect(Collectors.toList()));
  }

  @PostMapping("/submitSearchRequest")
  public ResponseEntity<SimulationCountResponse> submitSearchRequest(
      @RequestBody DataFilterRequest request) throws IOException, ParseException {
    return ResponseEntity.ok().body(entityFilterService.saveRequestAndCountResults(request));
  }

  @PostMapping("/updateSimulationRequest")
  public ResponseEntity<SimulationCountResponse> updateSimulationRequest(
      @RequestParam("simulationRequestId") String simulationRequestId,
      @RequestBody List<EntityTagRequest> request) {
    entityFilterService.updateRequestWithEntityTags(simulationRequestId, request);
    return ResponseEntity.status(HttpStatus.OK).build();
  }


  @GetMapping("/filter-sse")
  public SseEmitter filterEntities(
      @RequestParam("simulationRequestId") String simulationRequestId) {

    Optional<SimulationRequest> simulationRequestById = entityFilterService.getSimulationRequestById(
        simulationRequestId);

    if (simulationRequestById.isPresent()) {
      DataFilterRequest request = simulationRequestById.get().getRequest();
      LocationHierarchy locationHierarchy = locationHierarchyService.findByIdentifier(
          request.getHierarchyIdentifier());

      SseEmitter emitter = new SseEmitter(180000L);
      ExecutorService sseMvcExecutor = Executors.newScheduledThreadPool(2);
      Set<String> parents = new HashSet<>();

      List<AggregateHelper> aggregateHelpers = new ArrayList<>();

      sseMvcExecutor.execute(() -> {
        SearchHit lastResponse;

        try {
          do {
            FeatureSetResponseContainer featureSetResponse1 = entityFilterService.filterEntites(
                request, 1000, false, null);

            parents.addAll(featureSetResponse1.getFeatureSetResponse().getParents());

            aggregateHelpers.addAll(
                featureSetResponse1.getFeatureSetResponse().getFeatures().stream().map(
                        feature -> new AggregateHelper(feature.getIdentifier().toString(),
                            feature.getAncestry() == null ? new ArrayList<>() : feature.getAncestry(),
                            feature.getProperties().getMetadata(),
                            feature.getProperties().getGeographicLevel(), null))
                    .collect(Collectors.toList()));

            SseEventBuilder event = SseEmitter.event().data(FeatureSetResponse.builder()
                    .features(featureSetResponse1.getFeatureSetResponse().getFeatures())
                    .type(featureSetResponse1.getFeatureSetResponse().getType())
                    .identifier(featureSetResponse1.getFeatureSetResponse().getIdentifier()).build())
                .id(String.valueOf(UUID.randomUUID())).name("message");

            lastResponse = featureSetResponse1.getSearchHit();

            request.setLastHit(lastResponse);

            emitter.send(event);

          } while (lastResponse != null);

          int parentBatch = 3;

          List<Set<String>> parentBatches = splitArray(parents, parentBatch);

          SearchHit searchHit = null;
          for (Set<String> batch : parentBatches) {
            FeatureSetResponseContainer featureSetResponseContainer = entityFilterService.retrieveParentLocations(
                batch, request.getHierarchyIdentifier().toString(), searchHit);
            searchHit = featureSetResponseContainer.getSearchHit();
            emitter.send(SseEmitter.event().name("parent").id("")
                .data(featureSetResponseContainer.getFeatureSetResponse().getFeatures()));
          }

          // List<NodeNumber in Level,  TagName> - list of tags with the levels they are present in
          List<IndividualAggregateHelperType> individualAggregateHelperTypeList = aggregateHelpers.stream()
              .peek(aggregateHelper -> aggregateHelper.setFilteredNodeOrder(
                  locationHierarchy.getNodeOrder())).flatMap(
                  aggregateHelper -> aggregateHelper.getEntityMetadataResponses().stream().map(
                      entityMetadataResponse -> new IndividualAggregateHelper(
                          aggregateHelper.getFilteredNodeOrder()
                              .indexOf(aggregateHelper.getGeographicLevel()),
                          entityMetadataResponse))).map(
                  individualAggregateHelper -> new IndividualAggregateHelperType(
                      individualAggregateHelper.getIndex(),
                      individualAggregateHelper.getEntityMetadataResponse().getType()))
              .collect(Collectors.toList());

          // Map<Tag, IndividualAggregateHelperType(NodeNumber in Level,  TagName) - finding the lowest level a tag is present in
          Map<String, Optional<IndividualAggregateHelperType>> lowestLevelPerTag = individualAggregateHelperTypeList.stream()
              .collect(Collectors.groupingBy(IndividualAggregateHelperType::getType,
                  Collectors.reducing((a, b) -> {
                    if (b.getIndex() > a.getIndex()) {
                      return b;
                    } else {
                      return a;
                    }
                  })));

          // Map<Tag, List<EntityMetadataResponse> - Each metadata which belonged to the lowest levels per tag
          Map<String, List<EntityMetadataResponse>> collect4 = aggregateHelpers.stream().flatMap(
              aggregateHelper -> aggregateHelper.getEntityMetadataResponses().stream()
                  .filter(entityMetadataResponse -> {
                    if (lowestLevelPerTag.containsKey(entityMetadataResponse.getType())) {

                      Optional<IndividualAggregateHelperType> individualAggregateHelperType = lowestLevelPerTag.get(
                          entityMetadataResponse.getType());
                      return individualAggregateHelperType.map(
                          aggregateHelperType -> aggregateHelperType.getIndex().equals(
                              aggregateHelper.getFilteredNodeOrder()
                                  .indexOf(aggregateHelper.getGeographicLevel()))).orElse(false);
                    } else {
                      return false;
                    }
                  })).collect(Collectors.groupingBy(EntityMetadataResponse::getType));

          //stats - summation of tag items of the lowest levels - The summation of the lowest levels
          // of the result set represents the stats of the entire result set
          Map<String, Double> collect7 = collect4.entrySet().stream().map(
                  entry -> new SimpleEntry<>(entry.getKey(), entry.getValue().stream()
                      .mapToDouble(entityreposnse -> (double) entityreposnse.getValue()).sum()))
              .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

          emitter.send(SseEmitter.event().name("stats").id("").data(collect7));

          //        INPUT -> aggregateHelpers
          //        =====
          // LocationGrandParent - metadata=[tagABC-sum=7, tagDEF-sum=9] ancestry=LocationGrandParent
          //
          //    ,LocationParent1 metadata=[tagABC-sum=5] ancestry=LocationParent1,LocationGrandParent
          //    ,LocationParent2 metadata=[tagABC-sum=2, tagDEF-sum=5] ancestry=LocationParent2,LocationGrandParent
          //    ,LocationParent3 metadata=[tagABC-sum=2, tagDEF-sum=4] ancestry=LocationParent3,LocationGrandParent
          //
          //        ,Location1  metadata=[tagABC-sum=2] ancestry=Location1,LocationParent1,LocationGrandParent
          //        ,Location2  metadata=[tagABC-sum=3] ancestry=Location1,LocationParent1,LocationGrandParent
          //        ,Location3  metadata=[tagABC-sum=1] ancestry=Location1,LocationParent2,LocationGrandParent
          //        ,Location4  metadata=[tagABC-sum=1] ancestry=Location1,LocationParent2,LocationGrandParent
          //
          //         OUTPUT -> listsOfLowestLocationsWithTheirTags
          //         ======
          // LocationGrandParent - metadata=[] ancestry=LocationGrandParent
          //
          //    ,LocationParent1 metadata=[] ancestry=LocationParent1,LocationGrandParent
          //    ,LocationParent2 metadata=[tagDEF-sum=5] ancestry=LocationParent2,LocationGrandParent
          //    ,LocationParent3 metadata=[tagDEF-sum=4] ancestry=LocationParent3,LocationGrandParent
          //
          //        ,Location1  metadata=[tagABC-sum=2] ancestry=Location1,LocationParent1,LocationGrandParent
          //        ,Location2  metadata=[tagABC-sum=3] ancestry=Location1,LocationParent1,LocationGrandParent
          //        ,Location3  metadata=[tagABC-sum=1] ancestry=Location1,LocationParent2,LocationGrandParent
          //        ,Location4  metadata=[tagABC-sum=1] ancestry=Location1,LocationParent2,LocationGrandParent


          List<AggregateHelper> listsOfLowestLocationsWithTheirTags = aggregateHelpers.stream()
              .map(aggregateHelper -> {
                AggregateHelper helper = new AggregateHelper();
                helper.setAncestry(aggregateHelper.getAncestry());
                helper.setFilteredNodeOrder(aggregateHelper.getFilteredNodeOrder());
                helper.setEntityMetadataResponses(
                    aggregateHelper.getEntityMetadataResponses().stream()
                        .filter(entityMetadataResponse -> {
                          if (lowestLevelPerTag.containsKey(entityMetadataResponse.getType())) {
                            Optional<IndividualAggregateHelperType> individualAggregateHelperType = lowestLevelPerTag.get(
                                entityMetadataResponse.getType());

                            if (individualAggregateHelperType.isPresent()) {
                              return individualAggregateHelperType.get().getIndex().equals(
                                  aggregateHelper.getFilteredNodeOrder()
                                      .indexOf(aggregateHelper.getGeographicLevel()));
                            }
                            return false;
                          }
                          return false;
                        })

                        .collect(Collectors.toList()));
                helper.setIdentifier(aggregateHelper.getIdentifier());
                helper.setGeographicLevel(aggregateHelper.getGeographicLevel());
                return helper;
              }).collect(Collectors.toList());

          //         INPUT -> listsOfLowestLocationsWithTheirTags
          //         ======
          // LocationGrandParent - metadata=[] ancestry=LocationGrandParent
          //
          //    ,LocationParent1 metadata=[] ancestry=LocationParent1,LocationGrandParent
          //    ,LocationParent2 metadata=[tagDEF-sum=5] ancestry=LocationParent2,LocationGrandParent
          //    ,LocationParent3 metadata=[tagDEF-sum=4] ancestry=LocationParent3,LocationGrandParent
          //
          //        ,Location1  metadata=[tagABC-sum=2] ancestry=Location1,LocationParent1,LocationGrandParent
          //        ,Location2  metadata=[tagABC-sum=3] ancestry=Location1,LocationParent1,LocationGrandParent
          //        ,Location3  metadata=[tagABC-sum=1] ancestry=Location1,LocationParent2,LocationGrandParent
          //        ,Location4  metadata=[tagABC-sum=1] ancestry=Location1,LocationParent2,LocationGrandParent
          //
          //          OUTPUT -> collect8
          //
          //  Map: LocationParent1-LocationGrandParent metadataList=[metadata=[tagABC-sum=2], metadata=[tagABC-sum=3]]
          //  Map: LocationParent2-LocationGrandParent metadataList=[metadata=[tagABC-sum=1], metadata=[tagABC-sum=1]]
          //  Map: LocationGrandParent metadataList=[metadata=[tagDEF-sum=5], metadata=[tagDEF-sum=4]]


          Map<String, List<List<EntityMetadataResponse>>> collect8 = listsOfLowestLocationsWithTheirTags.stream()
              .map(lowestLocationWithTheirTags -> Pair.of(String.join(",",
                      lowestLocationWithTheirTags.getAncestry().size() > 1
                          ? lowestLocationWithTheirTags.getAncestry()
                          .subList(1, lowestLocationWithTheirTags.getAncestry().size())
                          : new ArrayList<>()),
                  lowestLocationWithTheirTags.getEntityMetadataResponses()))
              // LocationGrandParent - metadata=[] ancestry=LocationGrandParent -> Pair["" ,   metadata=[] ]
              //
              //    ,LocationParent1 metadata=[] ancestry=LocationParent1,LocationGrandParent -> Pair["LocationGrandParent" ,   metadata=[] ]
              //    ,LocationParent2 metadata=[tagDEF-sum=5] ancestry=LocationParent2,LocationGrandParent -> Pair["LocationGrandParent" ,   metadata=[tagDEF-sum=5] ]
              //    ,LocationParent3 metadata=[tagDEF-sum=4] ancestry=LocationParent3,LocationGrandParent -> Pair["LocationGrandParent" ,   metadata=[tagDEF-sum=4] ]
              //
              //        ,Location1  metadata=[tagABC-sum=2] ancestry=Location1,LocationParent1,LocationGrandParent -> Pair["LocationParent1-LocationGrandParent" ,   metadata=[tagABC-sum=2] ]
              //        ,Location2  metadata=[tagABC-sum=3] ancestry=Location1,LocationParent1,LocationGrandParent -> Pair["LocationParent1-LocationGrandParent" ,   metadata=[tagABC-sum=3] ]
              //        ,Location3  metadata=[tagABC-sum=1] ancestry=Location1,LocationParent2,LocationGrandParent -> Pair["LocationParent2-LocationGrandParent" ,   metadata=[tagABC-sum=1] ]
              //        ,Location4  metadata=[tagABC-sum=1] ancestry=Location1,LocationParent2,LocationGrandParent -> Pair["LocationParent2-LocationGrandParent" ,   metadata=[tagABC-sum=1] ]

              .collect(
                  Collectors.groupingBy(Pair::getFirst,
                      Collectors.mapping(Pair::getSecond, Collectors.toList())));

          //          INPUT -> collect8
          //
          //  Map: LocationParent1-LocationGrandParent metadataList=[metadata=[tagABC-sum=2], metadata=[tagABC-sum=3]]
          //  Map: LocationParent2-LocationGrandParent metadataList=[metadata=[tagABC-sum=1], metadata=[tagABC-sum=1]]
          //  Map: LocationGrandParent metadataList=[metadata=[tagDEF-sum=5], metadata=[tagDEF-sum=4]]
          //
          //          OUTPUT -> a
          //
          // Map: LocationParent1, metadataList=[metadata=[tagABC-sum=2], metadata=[tagABC-sum=3]]
          // Map: LocationParent2, metadataList=[metadata=[tagABC-sum=1], metadata=[tagABC-sum=1]]
          // Map: LocationGrandParent, metadataList=[metadata=[tagDEF-sum=5], metadata=[tagDEF-sum=4], metadata=[tagABC-sum=1], metadata=[tagABC-sum=1], metadata=[tagABC-sum=2], metadata=[tagABC-sum=3]]
          var a = collect8.entrySet().stream().map(entry -> Stream.of(entry.getKey().split(","))
                  .map(ancestor -> Pair.of(ancestor,
                      entry.getValue().stream().flatMap(Collection::stream)
                          .collect(Collectors.toList())))
                  .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (c, b) -> {
                    c.addAll(b);
                    return b;
                  }))).flatMap(map -> map.entrySet().stream())
              //Pair [LocationParent1, metadataList=[metadata=[tagABC-sum=2], metadata=[tagABC-sum=3]]
              //Pair [LocationParent2, metadataList=[metadata=[tagABC-sum=1], metadata=[tagABC-sum=1]]
              //Pair [LocationGrandParent, metadataList=[metadata=[tagDEF-sum=5], metadata=[tagDEF-sum=4]]
              //Pair [LocationGrandParent, metadataList=[metadata=[tagABC-sum=1], metadata=[tagABC-sum=1]]
              //Pair [LocationGrandParent, metadataList=[metadata=[tagABC-sum=2], metadata=[tagABC-sum=3]]
              .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (c, b) -> {
                c.addAll(b);
                return c;
              }));

          //Aggregation of the lowest level tags rolled up to parents in a map per tag in a map
          //          INPUT -> a
          //
          // Map: LocationParent1, metadataList=[metadata=[tagABC-sum=2], metadata=[tagABC-sum=3]]
          // Map: LocationParent2, metadataList=[metadata=[tagABC-sum=1], metadata=[tagABC-sum=1]]
          // Map: LocationGrandParent,
          //          metadataList=[metadata=[tagDEF-sum=5],
          //                        metadata=[tagDEF-sum=4],
          //                        metadata=[tagABC-sum=1],
          //                        metadata=[tagABC-sum=1],
          //                        metadata=[tagABC-sum=2],
          //                        metadata=[tagABC-sum=3]]
          //
          //          OUTPUT -> b
          //
          //Map:  LocationGrandParent, (Map: tagDEF-sum, val=9
          //                            Map: tagABC-sum, val=7)
          //Map:  LocationParent1, (Map: tagABC-sum, val=5)
          //Map:  LocationParent2, (Map: tagABC-sum, val=2)

          var b = a.entrySet().stream().map(entry -> new SimpleEntry<>(entry.getKey(),
                  entry.getValue().stream().collect(
                      Collectors.groupingBy(EntityMetadataResponse::getType,
                          Collectors.reducing(0, EntityMetadataResponse::getValue,
                              (c, d) -> (c instanceof Integer ? (Integer) c : (double) c) + (
                                  d instanceof Integer ? (Integer) d : (double) d))))))
              .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

          emitter.send(SseEmitter.event().name("aggregations").id("").data(b));

          emitter.send(SseEmitter.event().name("close").id("").data("close"));

        } catch (Exception ex) {
          emitter.completeWithError(ex);
        }
        emitter.complete();

      });

      return emitter;
    } else {
      return null;
    }
  }

  public static List<Set<String>> splitArray(Set<String> inputArray, int chunkSize) {
    return IntStream.iterate(0, i -> i + chunkSize)
        .limit((int) Math.ceil((double) inputArray.size() / chunkSize)).mapToObj(j -> new HashSet<>(
            Arrays.asList(Arrays.copyOfRange(inputArray.toArray(new String[0]), j,
                Math.min(inputArray.size(), j + chunkSize))))).collect(Collectors.toList());
  }


  @GetMapping("/inactive-locations")
  public SseEmitter inactiveLocations(
      @RequestParam("simulationRequestId") String simulationRequestId) {

    Optional<SimulationRequest> simulationRequestById = entityFilterService.getSimulationRequestById(
        simulationRequestId);

    if (simulationRequestById.isPresent()) {

      DataFilterRequest request = simulationRequestById.get().getRequest();

      request.setFilterGeographicLevelList(request.getInactiveGeographicLevelList());
      request.setEntityFilters(new ArrayList<>());
      request.setLocationIdentifier(null);

      SseEmitter emitter = new SseEmitter(180000L);
      ExecutorService sseMvcExecutor = Executors.newScheduledThreadPool(2);

      sseMvcExecutor.execute(() -> {
        SearchHit lastResponse;
        try {
          do {
            FeatureSetResponseContainer featureSetResponse1 = entityFilterService.filterEntites(
                request, 7000, false, null);

            SseEventBuilder event = SseEmitter.event()
                .data(featureSetResponse1.getFeatureSetResponse())
                .id(String.valueOf(UUID.randomUUID())).name("parent");

            lastResponse = featureSetResponse1.getSearchHit();

            request.setLastHit(lastResponse);

            emitter.send(event);

          } while (lastResponse != null);

          emitter.send(SseEmitter.event().name("close").id("").data("{}"));

        } catch (Exception ex) {
          emitter.completeWithError(ex);
        }
        emitter.complete();

      });

      return emitter;

    } else {
      return null;
    }
  }

  @GetMapping("/fullHierarchy")
  public FeatureSetResponse fullHierarchy(
      @RequestParam("hierarchyIdentifier") String hierarchyIdentifier)
      throws IOException, ParseException {

    DataFilterRequest request = new DataFilterRequest();
    request.setHierarchyIdentifier(UUID.fromString(hierarchyIdentifier));
    FeatureSetResponse featureSetResponse = new FeatureSetResponse();
    featureSetResponse.setFeatures(new ArrayList<>());
    featureSetResponse.setParents(new HashSet<>());
    featureSetResponse.setType("FeatureCollection");

    SearchHit lastResponse;
    do {
      try {

        FeatureSetResponseContainer featureSetResponse1 = entityFilterService.filterEntites(request,
            10000, false, null);

        featureSetResponse.getFeatures()
            .addAll(featureSetResponse1.getFeatureSetResponse().getFeatures());
        featureSetResponse.getParents()
            .addAll(featureSetResponse1.getFeatureSetResponse().getParents());
        lastResponse = featureSetResponse1.getSearchHit();
        request.setLastHit(lastResponse);


      } catch (Exception ex) {
        log.error("Error getting page");
        throw ex;
      }
    } while (lastResponse != null);

    return featureSetResponse;
  }

  @GetMapping("/fullHierarchyCSV")
  public ResponseEntity<Resource> fullHierarchyCSV(
      @RequestParam("hierarchyIdentifier") String hierarchyIdentifier,
      @RequestParam("fileName") String fileName, @RequestParam("delimiter") char delimiter)
      throws IOException, ParseException {

    StringWriter stringWriter = new StringWriter();

    BeanInstructions instructions = new BeanInstructionsImpl(LocationCSVRecord.class);
    instructions.setSeparator(delimiter);
    instructions.setUseHeader(false);

    DataFilterRequest request = new DataFilterRequest();
    request.setHierarchyIdentifier(UUID.fromString(hierarchyIdentifier));
    FeatureSetResponse featureSetResponse = new FeatureSetResponse();
    featureSetResponse.setFeatures(new ArrayList<>());
    featureSetResponse.setParents(new HashSet<>());
    featureSetResponse.setType("FeatureCollection");

    SearchHit lastResponse;
    do {
      try {

        log.info("calling filterEntites");
        FeatureSetResponseContainer featureSetResponse1 = entityFilterService.filterEntites(request,
            10000, true, List.of("geometry"));
        log.info("called filterEntites");

        featureSetResponse.getFeatures()
            .addAll(featureSetResponse1.getFeatureSetResponse().getFeatures());
        featureSetResponse.getParents()
            .addAll(featureSetResponse1.getFeatureSetResponse().getParents());
        lastResponse = featureSetResponse1.getSearchHit();
        request.setLastHit(lastResponse);

      } catch (Exception ex) {
        log.error("Error getting page");
        throw ex;
      }
    } while (lastResponse != null);

    Map<String, Map<String, Object>> featureMap = featureSetResponse.getFeatures().stream().map(
            featureSet -> new SimpleEntry<>(featureSet.getIdentifier().toString(),
                featureSet.getProperties().getMetadata().stream().collect(
                    Collectors.toMap(EntityMetadataResponse::getType, EntityMetadataResponse::getValue,
                        (a, b) -> b))))
        .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue, (a, b) -> b));

    Set<String> tags = featureSetResponse.getFeatures().stream().flatMap(
        feature -> feature.getProperties().getMetadata().stream()
            .map(EntityMetadataResponse::getType)).collect(Collectors.toSet());

    List<LocationCSVRecord> locationList = featureSetResponse.getFeatures().stream().map(
        feature -> LocationCSVRecordFactory.getLocationCSVRecordFromLocationResponse(feature,
            tags.stream().map(tag -> new SimpleEntry<>(tag,
                    featureMap.get(feature.getIdentifier().toString()).getOrDefault(tag, "")))
                .collect(Collectors.toSet()))).collect(Collectors.toList());

    RowWriter rowWriter = new RowWriterImpl(stringWriter);

    List<String> headerArr = new ArrayList<>();
    headerArr.add("Identifier");
    headerArr.add("Name");
    headerArr.add("GeographicLevel");
    headerArr.addAll(tags);
    rowWriter.writeHeader(headerArr.toArray(new String[0]));

    locationList.stream().forEach(location -> {
      List<String> strArr = new ArrayList<>();
      strArr.add(location.getIdentifier());
      strArr.add(location.getName());
      strArr.add(location.getGeographicLevel());

      List<String> collect = location.getMeta().stream().map(SimpleEntry::getValue).map(val -> {
        if (val instanceof Double) {
          return ((Double) val).toString();
        } else {
          return (String) val;
        }
      }).collect(Collectors.toList());

      strArr.addAll(collect);

      rowWriter.writeRow(strArr.toArray(new String[0]));
    });

    stringWriter.close();
    InputStream targetStream = new ByteArrayInputStream(stringWriter.toString().getBytes());
    InputStreamResource resource = new InputStreamResource(targetStream);

    return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-disposition", "attachment;filename=" + fileName).body(resource);
  }


  @GetMapping("/person/{personIdentifier}")
  public ResponseEntity<PersonMainData> getPersonDetails(@PathVariable UUID personIdentifier)
      throws IOException {
    return ResponseEntity.ok().body(entityFilterService.getPersonsDetails(personIdentifier));
  }

  @PutMapping
  public ResponseEntity<Void> updateTag(@RequestBody UpdateEntityTagRequest request) {
    entityTagService.updateEntityTag(request);
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @PostMapping("/saveSimulationHierarchy")
  public ResponseEntity<SaveHierarchyResponse> saveSimulationHierarchy(RequestEntity<SaveHierarchyRequest> saveHierarchyRequest){

    log.info("{}",saveHierarchyRequest);

    return ResponseEntity.ok(SaveHierarchyResponse.builder().identifier(1).name("test").build());
  }
}

@Setter
@Getter
@AllArgsConstructor
class LocationMetadatHolder {

  private UUID identifier;
  private List<String> ancestors;
  private List<EntityMetadataResponse> entityMetadataResponses;
  private Map<String, Object> aggregates;
}

@Setter
@Getter
@AllArgsConstructor
class IndividualAggregateHelper {


  private Integer index;
  private EntityMetadataResponse entityMetadataResponse;

}

@Setter
@Getter
@AllArgsConstructor
class IndividualAggregateHelperType {


  private Integer index;
  private String type;

}