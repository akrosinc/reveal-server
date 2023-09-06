package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.LookupEntityTagResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.DataFilterRequest;
import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;
import com.revealprecision.revealserver.api.v1.dto.request.UpdateEntityTagRequest;
import com.revealprecision.revealserver.api.v1.dto.response.ComplexTagDto;
import com.revealprecision.revealserver.api.v1.dto.response.EntityTagResponse;
import com.revealprecision.revealserver.api.v1.dto.response.FeatureSetResponse;
import com.revealprecision.revealserver.api.v1.dto.response.LookupEntityTypeResponse;
import com.revealprecision.revealserver.api.v1.dto.response.PersonMainData;
import com.revealprecision.revealserver.api.v1.dto.response.SimulationCountResponse;
import com.revealprecision.revealserver.persistence.domain.ComplexTag;
import com.revealprecision.revealserver.persistence.repository.GeneratedHierarchyRepository;
import com.revealprecision.revealserver.service.EntityFilterEsService;
import com.revealprecision.revealserver.service.EntityTagService;
import com.revealprecision.revealserver.service.EventAggregationService;
import com.revealprecision.revealserver.service.LocationHierarchyService;
import com.revealprecision.revealserver.service.LookupEntityTypeService;
import io.swagger.v3.oas.annotations.Operation;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
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
  private final GeneratedHierarchyRepository generatedHierarchyRepository;

  @Operation(summary = "Create Tag", description = "Create Tag", tags = {"Entity Tags"})
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<EntityTagResponse> createTag(
      @Valid @RequestBody EntityTagRequest entityTagRequest) {

    if (entityTagRequest.getTags() != null && entityTagRequest.getTags().size() > 0) {

      if (entityTagRequest.getTags().size() == 1) {
        entityTagRequest.setTag(entityTagRequest.getTags().get(0).getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(EntityTagResponseFactory.fromEntity(
            entityTagService.createEntityTag(entityTagRequest, true)));
      } else {
        entityTagService.createEntityTagsSkipExisting(entityTagRequest, true);
        return ResponseEntity.accepted().build();
      }

    }
    return ResponseEntity.badRequest().build();

  }


  @Operation(summary = "Get All Entity Tags", description = "Get All Entity Tags", tags = {
      "Entity Tags"})
  @GetMapping(value = "/page", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Page<EntityTagResponse>> getAll(Pageable pageable,
      @RequestParam(name = "filter", defaultValue = "all") String filter,
      @RequestParam(name = "search", defaultValue = "", required = false) String search) {

    switch (filter) {

      case "importable":
        return ResponseEntity.ok(
            EntityTagResponseFactory.fromEntityPage(
                entityTagService.getAllPagedNonAggregateEntityTags(pageable, search), pageable));
      case "all":
      default:
        return ResponseEntity.ok(
            EntityTagResponseFactory.fromEntityPage(
                entityTagService.getOrSearchAllEntityTagsPaged(pageable, search), pageable));
    }
  }

  @GetMapping(value = "/dataAssociated/{hierarchyIdentifier}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<EntityTagResponse>> getDataAssociatedEntityTags(
      @PathVariable String hierarchyIdentifier) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(entityTagService.getAllAggregateEntityTagsAssociatedToData(hierarchyIdentifier));

  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<EntityTagResponse>> getTagsByEntityType(
      @RequestParam(name = "filter", defaultValue = "all") String filter) {

    List<EntityTagResponse> coreFields = new ArrayList<>();
    switch (filter) {
      case "aggregate":
        coreFields.addAll(
            entityTagService.getAllAggregateEntityTags().stream()
                .map(EntityTagResponseFactory::fromEntity)
                .collect(Collectors.toList()));
        break;
      case "importable":
        coreFields.addAll(
            entityTagService.getAllNonAggregateEntityTags().stream()
                .map(EntityTagResponseFactory::fromEntity)
                .collect(Collectors.toList()));
        break;
      case "all":
      default:
        coreFields.addAll(
            entityTagService.getAllEntityTags()
                .stream().map(EntityTagResponseFactory::fromEntity).collect(Collectors.toList())
        );
        break;
    }

    return ResponseEntity.status(HttpStatus.OK).body(coreFields);
  }

  @GetMapping("/eventBasedTags")
  public ResponseEntity<List<EntityTagResponse>> getEventBasedTags() {

    return ResponseEntity.status(HttpStatus.OK)
        .body(eventAggregationService.getEventBasedTags());
  }

  @GetMapping("/entityType")
  public ResponseEntity<List<LookupEntityTypeResponse>> getEntityTypes() {
    return ResponseEntity.status(HttpStatus.OK).body(
        lookupEntityTypeService.getAllLookUpEntityTypes().stream()
            .map(LookupEntityTagResponseFactory::fromEntity).collect(Collectors.toList()));
  }

  @PostMapping("/submitSearchRequest")
  public ResponseEntity<SimulationCountResponse> submitSearchRequest(
      @RequestBody DataFilterRequest request) {
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
    return entityFilterService.getSseEmitter(simulationRequestId);
  }


  @GetMapping("/inactive-locations")
  public SseEmitter inactiveLocations(
      @RequestParam("simulationRequestId") String simulationRequestId) {
    return entityFilterService.getInactiveLocationsSseEmitter(simulationRequestId);
  }

  @GetMapping("/fullHierarchy")
  public FeatureSetResponse fullHierarchy(
      @RequestParam("hierarchyIdentifier") String hierarchyIdentifier)
      throws IOException, ParseException {
    return entityFilterService.getFullHierarchy(hierarchyIdentifier);
  }

  @GetMapping("/fullHierarchyCSV")
  public ResponseEntity<Resource> fullHierarchyCSV(
      @RequestParam("hierarchyIdentifier") String hierarchyIdentifier,
      @RequestParam("fileName") String fileName, @RequestParam("delimiter") char delimiter)
      throws IOException, ParseException {

    InputStreamResource resource = entityFilterService.getFullHierarchyCSV(
        hierarchyIdentifier, delimiter);

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

  @GetMapping("/complex")
  public ResponseEntity<List<ComplexTagDto>> getComplexTag(){
    return ResponseEntity.ok(entityTagService.getAllComplexTags().stream().map(complexTag -> ComplexTagDto.builder()
        .hierarchyId(complexTag.getHierarchyId())
        .formula(complexTag.getFormula())
        .tags(complexTag.getTags())
        .hierarchyType(complexTag.getHierarchyType())
        .tagName(complexTag.getTagName())
        .id(String.valueOf(complexTag.getId()))
        .build())
        .collect(Collectors.toList()));
  }

  @PostMapping("/complex")
  public ResponseEntity<ComplexTagDto> getComplexTag(@RequestBody ComplexTagDto request){
    ComplexTag complexTag = entityTagService.saveComplexTag(ComplexTag.builder()
        .formula(request.getFormula())
        .hierarchyId(request.getHierarchyId())
        .hierarchyType(request.getHierarchyType())
        .tagName(request.getTagName())
        .tags(request.getTags())
        .build());
    return ResponseEntity.ok(
        ComplexTagDto.builder()
            .id(String.valueOf(complexTag.getId()))
            .formula(complexTag.getFormula())
            .hierarchyId(complexTag.getHierarchyId())
            .hierarchyType(complexTag.getHierarchyType())
            .tags(complexTag.getTags())
            .tagName(complexTag.getTagName())
            .build()
    );
  }


}


