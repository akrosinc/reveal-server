package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagEventFactory;
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
import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent.OrgGrant;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent.UserGrant;
import com.revealprecision.revealserver.persistence.domain.ComplexTag;
import com.revealprecision.revealserver.persistence.domain.ComplexTagAccGrantsOrganization;
import com.revealprecision.revealserver.persistence.domain.ComplexTagAccGrantsUser;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.EntityTagAccGrantsOrganization;
import com.revealprecision.revealserver.persistence.domain.EntityTagAccGrantsUser;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.repository.OrganizationRepository;
import com.revealprecision.revealserver.persistence.repository.UserRepository;
import com.revealprecision.revealserver.service.EntityFilterEsService;
import com.revealprecision.revealserver.service.EntityTagService;
import com.revealprecision.revealserver.service.EventAggregationService;
import com.revealprecision.revealserver.service.LookupEntityTypeService;
import io.swagger.v3.oas.annotations.Operation;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
  private final OrganizationRepository organizationRepository;
  private final UserRepository userRepository;


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
  public ResponseEntity<Page<EntityTagEvent>> getAll(Pageable pageable,
      @RequestParam(name = "filter", defaultValue = "all") String filter,
      @RequestParam(name = "search", defaultValue = "", required = false) String search) {

    switch (filter) {

      case "importable":
        Page<EntityTag> allPagedNonAggregateEntityTags = entityTagService.getAllPagedNonAggregateEntityTags(
            pageable, search);

        List<UUID> orgIds = allPagedNonAggregateEntityTags.stream().flatMap(
            entityTag -> entityTag.getEntityTagAccGrantsOrganizations().stream()
                .map(EntityTagAccGrantsOrganization::getOrganizationId)).collect(
            Collectors.toList());

        List<UUID> userIds = allPagedNonAggregateEntityTags.stream().flatMap(
            entityTag -> entityTag.getEntityTagAccGrantsUsers().stream()
                .map(EntityTagAccGrantsUser::getUserSid)).collect(
            Collectors.toList());

        Set<Organization> orgGrants = organizationRepository.findByIdentifiers(orgIds);
        Set<User> userGrants = userRepository.findBySidIn(userIds);

        return ResponseEntity.ok(
            new PageImpl<>(allPagedNonAggregateEntityTags.stream().map(entityTag ->
                EntityTagEventFactory.getEntityTagEventWithGrantData(entityTag, orgGrants,
                    userGrants)
            ).collect(Collectors.toList()), pageable,
                allPagedNonAggregateEntityTags.getTotalElements()));
      case "all":
      default:
        Page<EntityTag> orSearchAllEntityTagsPaged = entityTagService.getOrSearchAllEntityTagsPaged(
            pageable, search);

        List<UUID> orgIds2 = orSearchAllEntityTagsPaged.stream().flatMap(
            entityTag -> entityTag.getEntityTagAccGrantsOrganizations().stream()
                .map(EntityTagAccGrantsOrganization::getOrganizationId)).collect(
            Collectors.toList());

        List<UUID> userIds2 = orSearchAllEntityTagsPaged.stream().flatMap(
            entityTag -> entityTag.getEntityTagAccGrantsUsers().stream()
                .map(EntityTagAccGrantsUser::getUserSid)).collect(
            Collectors.toList());

        Set<Organization> orgGrants2 = organizationRepository.findByIdentifiers(orgIds2);
        Set<User> userGrants2 = userRepository.findBySidIn(userIds2);

        return ResponseEntity.ok(new PageImpl<>(orSearchAllEntityTagsPaged.stream().map(entityTag ->
            EntityTagEventFactory.getEntityTagEventWithGrantData(entityTag, orgGrants2, userGrants2)
        ).collect(Collectors.toList()), pageable, orSearchAllEntityTagsPaged.getTotalElements()));
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
  public ResponseEntity<?> updateSimulationRequest(
      @RequestParam("simulationRequestId") String simulationRequestId,
      @RequestBody List<EntityTagRequest> request) {
    try {
      entityFilterService.updateRequestWithEntityTags(simulationRequestId, request);
    } catch (AuthorizationDeniedException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
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
  public ResponseEntity<List<ComplexTagDto>> getComplexTag() {
    List<ComplexTag> allComplexTags = entityTagService.getAllComplexTags();

    List<ComplexTagDto> collect = getComplexTagDtos(
        allComplexTags);

    return ResponseEntity.ok(collect);
  }

  private List<ComplexTagDto> getComplexTagDtos(List<ComplexTag> allComplexTags) {
    List<UUID> orgIds = allComplexTags.stream()
        .flatMap(complexTag -> complexTag.getComplexTagAccGrantsOrganizations().stream().map(
            ComplexTagAccGrantsOrganization::getOrganizationId)).collect(Collectors.toList());

    List<UUID> userIds = allComplexTags.stream()
        .flatMap(complexTag -> complexTag.getComplexTagAccGrantsUsers().stream().map(
            ComplexTagAccGrantsUser::getUserSid)).collect(Collectors.toList());

    Set<Organization> orgGrants = organizationRepository.findByIdentifiers(orgIds);
    Set<User> userGrants = userRepository.findBySidIn(userIds);

    List<ComplexTagDto> collect = allComplexTags.stream().map(complexTag -> {
      List<OrgGrant> orgGrantObj = orgGrants.stream().filter(
              organization -> complexTag.getComplexTagAccGrantsOrganizations().stream().map(
                  ComplexTagAccGrantsOrganization::getOrganizationId).collect(
                  Collectors.toList()).contains(organization.getIdentifier()))
          .map(organization -> new OrgGrant(organization.getIdentifier(), organization.getName()))
          .collect(
              Collectors.toList());
      List<UserGrant> userGrantObj = userGrants.stream().filter(
              user -> complexTag.getComplexTagAccGrantsUsers().stream().map(
                  ComplexTagAccGrantsUser::getUserSid).collect(
                  Collectors.toList()).contains(user.getSid()))
          .map(user -> new UserGrant(user.getSid(), user.getUsername()))
          .collect(
              Collectors.toList());
      return ComplexTagDto.builder()
          .hierarchyId(complexTag.getHierarchyId())
          .formula(complexTag.getFormula())
          .tags(complexTag.getTags())
          .hierarchyType(complexTag.getHierarchyType())
          .tagName(complexTag.getTagName())
          .id(String.valueOf(complexTag.getId()))
          .tagAccGrantsOrganization(orgGrantObj)
          .tagAccGrantsUser(userGrantObj)
          .isPublic(complexTag.isPublic())
          .build();

    }).collect(Collectors.toList());
    return collect;
  }

  @PostMapping("/complex")
  public ResponseEntity<ComplexTagDto> getComplexTag(@RequestBody ComplexTagDto request) {
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

  @PostMapping("/updateGrants")
  public ResponseEntity<List<EntityTagEvent>> getComplexTag(
      @RequestBody List<EntityTagRequest> request) {

    List<EntityTag> entityTags = entityTagService.updateEntityTagAccessGrants(request);

    return ResponseEntity.ok(entityTags.stream().map(EntityTagEventFactory::getEntityTagEvent)
        .collect(Collectors.toList()));

  }

  @PostMapping("/updateComplexTagGrants")
  public ResponseEntity<List<ComplexTagDto>> updateComplexTag(
      @RequestBody List<ComplexTagDto> request) {

    List<ComplexTag> entityTags = entityTagService.updateComplexTagAccessGrants(request);

    return ResponseEntity.ok(getComplexTagDtos(entityTags));

  }


}


