package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.csv.LocationCSVRecord;
import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.LookupEntityTagResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.DataFilterRequest;
import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;
import com.revealprecision.revealserver.api.v1.dto.request.UpdateEntityTagRequest;
import com.revealprecision.revealserver.api.v1.dto.response.AggregateHelper;
import com.revealprecision.revealserver.api.v1.dto.response.EntityMetadataResponse;
import com.revealprecision.revealserver.api.v1.dto.response.EntityTagResponse;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponse;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponseContainer;
import com.revealprecision.revealserver.api.v1.dto.response.LocationResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LookupEntityTypeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PersonMainData;
import com.revealprecision.revealserver.api.v1.dto.response.SimulationCountResponse;
import com.revealprecision.revealserver.api.v1.facade.factory.LocationCSVRecordFactory;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import com.revealprecision.revealserver.persistence.domain.SimulationRequest;
import com.revealprecision.revealserver.service.EntityFilterService;
import com.revealprecision.revealserver.service.EntityTagService;
import com.revealprecision.revealserver.service.EventAggregationService;
import com.revealprecision.revealserver.service.LookupEntityTypeService;
import io.swagger.v3.oas.annotations.Operation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.csveed.bean.BeanInstructions;
import org.csveed.bean.BeanInstructionsImpl;
import org.csveed.row.RowWriter;
import org.csveed.row.RowWriterImpl;
import org.elasticsearch.search.SearchHit;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
public class EntityTagController {

  private final EntityTagService entityTagService;
  private final EntityFilterService entityFilterService;
  private final LookupEntityTypeService lookupEntityTypeService;
  private final EventAggregationService eventAggregationService;

  @Operation(summary = "Create Tag", description = "Create Tag", tags = {"Entity Tags"})
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<EntityTagResponse> createTag(
      @Valid @RequestBody EntityTagRequest entityTagRequest) {
    entityTagRequest.setAddToMetadata(true); //TODO set this value on the frontend and remove this
    return ResponseEntity.status(HttpStatus.CREATED).body(
        EntityTagResponseFactory.fromEntity(
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
        return ResponseEntity.status(HttpStatus.OK)
            .body(EntityTagResponseFactory.fromEntityPage(
                entityTagService.getAllPagedGlobalEntityTags(pageable, search), pageable));
      case "importable":
        return ResponseEntity.status(HttpStatus.OK)
            .body(EntityTagResponseFactory.fromEntityPage(
                entityTagService.getAllPagedGlobalNonAggregateEntityTags(pageable, search),
                pageable));
      case "all":
      default:
        return ResponseEntity.status(HttpStatus.OK)
            .body(
                EntityTagResponseFactory.fromEntityPage(
                    entityTagService.getAllPagedEntityTags(pageable, search),
                    pageable));
    }
  }


  @GetMapping("/{entityTypeIdentifier}")
  public ResponseEntity<List<EntityTagResponse>> getTagsByEntityType(
      @PathVariable UUID entityTypeIdentifier,
      @RequestParam(name = "filter", defaultValue = "all") String filter) {

    LookupEntityType lookupEntityType = lookupEntityTypeService.getLookUpEntityTypeById(
        entityTypeIdentifier);

    List<EntityTagResponse> coreFields = lookupEntityType.getCoreFields().stream()
        .map(EntityTagResponseFactory::fromCoreField)
        .collect(Collectors.toList());

    switch (filter) {
      case "global":
        coreFields.addAll(entityTagService.getAllGlobalEntityTagsByLookupEntityTypeIdentifier(
                entityTypeIdentifier).stream().map(EntityTagResponseFactory::fromEntity)
            .collect(
                Collectors.toList()));
        break;

      case "importable":
        coreFields.addAll(
            entityTagService.getAllGlobalNonAggregateEntityTagsByLookupEntityTypeIdentifier(
                    entityTypeIdentifier).stream().map(EntityTagResponseFactory::fromEntity)
                .collect(
                    Collectors.toList()));
        break;
      case "all":
      default:
        coreFields.addAll(
            entityTagService.getEntityTagsByLookupEntityTypeIdentifier(
                    entityTypeIdentifier).stream().map(EntityTagResponseFactory::fromEntity)
                .collect(
                    Collectors.toList()));
        break;
    }

    return ResponseEntity.status(HttpStatus.OK)
        .body(
            coreFields);
  }

  @GetMapping("/eventBasedTags/{entityTypeIdentifier}")
  public ResponseEntity<List<EntityTagResponse>> getEventBasedTags(@PathVariable UUID entityTypeIdentifier) {

    return ResponseEntity.status(HttpStatus.OK)
        .body(
            eventAggregationService.getEventBasedTags(entityTypeIdentifier));
  }

  @GetMapping("/entityType")
  public ResponseEntity<List<LookupEntityTypeResponse>> getEntityTypes() {
    return ResponseEntity.status(HttpStatus.OK)
        .body(lookupEntityTypeService.getAllLookUpEntityTypes().stream().map(
            LookupEntityTagResponseFactory::fromEntity).collect(Collectors.toList()));
  }

  @PostMapping("/submitSearchRequest")
  public ResponseEntity<SimulationCountResponse> submitSearchRequest(
      @RequestBody DataFilterRequest request)
      throws IOException, ParseException {
    return ResponseEntity.ok().body(entityFilterService.saveRequestAndCountResults(request));
  }


  @GetMapping("/filter-sse")
  public SseEmitter filterEntities(@RequestParam("simulationRequestId") String simulationRequestId) {

    Optional<SimulationRequest> simulationRequestById = entityFilterService.getSimulationRequestById(
        simulationRequestId);

    if (simulationRequestById.isPresent()) {
      DataFilterRequest request = simulationRequestById.get().getRequest();

      SseEmitter emitter = new SseEmitter(180000L);
      ExecutorService sseMvcExecutor = Executors.newScheduledThreadPool(2);
      Set<String> parents = new HashSet<>();

      List<AggregateHelper> aggregateHelpers = new ArrayList<>();

      sseMvcExecutor.execute(() -> {
        SearchHit lastResponse;

        try {
          do {
            FeatureSetResponseContainer featureSetResponse1 = entityFilterService.filterEntites(
                request, 7000,false,null);

            parents.addAll(featureSetResponse1.getFeatureSetResponse().getParents());

            aggregateHelpers.addAll(
                featureSetResponse1.getFeatureSetResponse().getFeatures().stream().map(feature ->
                    new AggregateHelper(feature.getIdentifier().toString(),
                        feature.getAncestry() == null ? new ArrayList<>() : feature.getAncestry(),
                        feature.getProperties().getMetadata(),
                        feature.getProperties().getGeographicLevel()
                        , null
                    )
                ).collect(Collectors.toList()));

            SseEventBuilder event = SseEmitter.event()
                .data(FeatureSetResponse.builder()
                    .features(featureSetResponse1.getFeatureSetResponse().getFeatures())
                    .type(featureSetResponse1.getFeatureSetResponse().getType())
                    .identifier(featureSetResponse1.getFeatureSetResponse().getIdentifier())
                    .build()
                )
                .id(String.valueOf(UUID.randomUUID()))
                .name("message");

            lastResponse = featureSetResponse1.getSearchHit();

            request.setLastHit(lastResponse);

            emitter.send(event);

          } while (lastResponse != null);

          List<LocationResponse> locationResponses = entityFilterService.retrieveParentLocations(
              parents, request.getHierarchyIdentifier().toString());

          List<SimpleEntry<String, List<EntityMetadataResponse>>> collect = aggregateHelpers.stream()
              .flatMap(aggregateHelper -> aggregateHelper.getAncestry().stream().map(
                  ancestor -> new SimpleEntry<>(ancestor,
                      aggregateHelper.getEntityMetadataResponses()))).collect(Collectors.toList());

          Map<String, List<List<EntityMetadataResponse>>> collect1 = collect.stream().collect(
              Collectors.groupingBy(SimpleEntry::getKey,
                  Collectors.mapping(SimpleEntry::getValue, Collectors.toList())));

          Map<String, Map<String, Object>> collect2 = collect1.entrySet().stream().map(
              locationEntry -> {

                List<EntityMetadataResponse> collect5 = locationEntry.getValue().stream()
                    .flatMap(Collection::stream).collect(
                        Collectors.toList());

                Map<String, List<Object>> collect6 = collect5.stream().collect(
                    Collectors.groupingBy(EntityMetadataResponse::getType,
                        Collectors.mapping(EntityMetadataResponse::getValue, Collectors.toList())));

                Map<String, Object> collect3 = collect6.entrySet().stream().map(entry -> {

                  Object reduce = entry.getValue().stream()
                      .reduce(0d, (subtotal, newVal) -> (Double) newVal + (Double) subtotal);

                  return new SimpleEntry<>(entry.getKey(), reduce);
                }).collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

                return new SimpleEntry<>(locationEntry.getKey(), collect3);
              }
          ).collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

          List<LocationResponse> collect3 = locationResponses.stream().map(locationResponse -> {
            locationResponse.setAggregates(
                collect2.get(locationResponse.getIdentifier().toString()));
            return locationResponse;
          }).collect(Collectors.toList());

          emitter.send(SseEmitter.event().name("close").id("").data(collect3));

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
                request, 7000, false,null);

            SseEventBuilder event = SseEmitter.event()
                .data(featureSetResponse1.getFeatureSetResponse())
                .id(String.valueOf(UUID.randomUUID()))
                .name("parent");

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

        FeatureSetResponseContainer featureSetResponse1 = entityFilterService.filterEntites(
            request, 10000, false, null);

        featureSetResponse.getFeatures()
            .addAll(featureSetResponse1.getFeatureSetResponse().getFeatures());
        featureSetResponse.getParents().addAll(featureSetResponse1.getFeatureSetResponse()
            .getParents());
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
        FeatureSetResponseContainer featureSetResponse1 = entityFilterService.filterEntites(
            request, 10000,true,List.of("geometry"));
        log.info("called filterEntites");

        featureSetResponse.getFeatures()
            .addAll(featureSetResponse1.getFeatureSetResponse().getFeatures());
        featureSetResponse.getParents().addAll(featureSetResponse1.getFeatureSetResponse()
            .getParents());
        lastResponse = featureSetResponse1.getSearchHit();
        request.setLastHit(lastResponse);

      } catch (Exception ex) {
        log.error("Error getting page");
        throw ex;
      }
    } while (lastResponse != null );

    Map<String, Map<String, Object>> featureMap = featureSetResponse.getFeatures().stream()
        .map(featureSet -> new SimpleEntry<>(featureSet.getIdentifier().toString(),
            featureSet.getProperties().getMetadata().stream()
                .collect(Collectors.toMap(EntityMetadataResponse::getType,
                    EntityMetadataResponse::getValue, (a, b) -> b))))
        .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue, (a, b) -> b));

    Set<String> tags = featureSetResponse.getFeatures().stream().flatMap(
            feature -> feature.getProperties().getMetadata().stream()
                .map(EntityMetadataResponse::getType))
        .collect(Collectors.toSet());

    List<LocationCSVRecord> locationList = featureSetResponse.getFeatures().stream()
        .map(feature -> LocationCSVRecordFactory.getLocationCSVRecordFromLocationResponse(
            feature, tags.stream()
            .map(tag -> new SimpleEntry<>(tag,
                featureMap.get(feature.getIdentifier().toString()).getOrDefault(tag, "")))
            .collect(Collectors.toSet()))).collect(Collectors.toList());

    RowWriter  rowWriter = new RowWriterImpl(stringWriter);

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

      List<String> collect = location.getMeta().stream().map(SimpleEntry::getValue).map(val ->
      {
        if (val instanceof Double){
          return ((Double)val).toString();
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

    return ResponseEntity.status(HttpStatus.OK)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-disposition", "attachment;filename=" + fileName)
        .body(resource);
  }


  @GetMapping("/person/{personIdentifier}")
  public ResponseEntity<PersonMainData> getPersonDetails(@PathVariable UUID personIdentifier)
      throws IOException {
    return ResponseEntity.ok().body(entityFilterService.getPersonsDetails(personIdentifier));
  }

  @PutMapping
  public ResponseEntity<Void> updateTag(
      @RequestBody UpdateEntityTagRequest request) {
    entityTagService.updateEntityTag(request);
    return ResponseEntity.status(HttpStatus.OK).build();
  }
}

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
class Ancestor {

  String identifier;
  String level;
}

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
class LocationAncestorContainer {

  String identifier;
  String level;
  List<Ancestor> ancestors;
}

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
class AncestorList {

  String level;
  TreeSet<Ancestor> ancestors;
}


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
class LocationAncestorContainerMega {

  String identifier;
  String level;
  List<LocationAncestorContainerLevel> ancestors;
}


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
class LocationAncestorContainerLevel {

  Ancestor level;
  List<List<Ancestor>> ancestors;
}

