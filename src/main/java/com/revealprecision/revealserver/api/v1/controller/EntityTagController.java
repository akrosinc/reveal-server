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
import com.revealprecision.revealserver.persistence.domain.ComplexTagOwnership;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.EntityTagAccGrantsOrganization;
import com.revealprecision.revealserver.persistence.domain.EntityTagAccGrantsUser;
import com.revealprecision.revealserver.persistence.domain.EntityTagOwnership;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.repository.OrganizationRepository;
import com.revealprecision.revealserver.persistence.repository.UserRepository;
import com.revealprecision.revealserver.service.EntityFilterEsService;
import com.revealprecision.revealserver.service.EntityTagService;
import com.revealprecision.revealserver.service.EventAggregationService;
import com.revealprecision.revealserver.service.KeycloakService;
import com.revealprecision.revealserver.service.LookupEntityTypeService;
import com.revealprecision.revealserver.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
  private final UserService userService;
  private final KeycloakService keycloakService;


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

    User currentUser = userService.getCurrentUser();

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

        List<UUID> ownerIds = allPagedNonAggregateEntityTags.stream()
            .flatMap(entityTag -> entityTag.getOwners().stream().map(
                EntityTagOwnership::getUserSid)).collect(
                Collectors.toList());

        Set<Organization> orgGrants = organizationRepository.findByIdentifiers(orgIds);
        Set<User> userGrants = userRepository.findBySidIn(userIds);
        Set<User> owners = userRepository.findBySidIn(ownerIds);

        return ResponseEntity.ok(
            new PageImpl<>(allPagedNonAggregateEntityTags.stream().map(entityTag ->
                EntityTagEventFactory.getEntityTagEventWithGrantData(entityTag, orgGrants,
                    userGrants, currentUser, owners)
            ).collect(Collectors.toList()), pageable,
                allPagedNonAggregateEntityTags.getTotalElements()));
      case "all":
      default:
        Page<EntityTag> orSearchAllEntityTagsPaged = entityTagService.getAllPagedNonAggregateEntityTags(
            pageable, search);

        List<EntityTag> tags = new ArrayList<>(orSearchAllEntityTagsPaged.getContent());

        Set<EntityTag> aggregateTags = entityTagService.findEntityTagsByReferencedTagIn(
            orSearchAllEntityTagsPaged.getContent().stream().map(EntityTag::getIdentifier).collect(
                Collectors.toList()));

        tags.addAll(aggregateTags);

        List<UUID> orgIds2 = orSearchAllEntityTagsPaged.stream().flatMap(
            entityTag -> entityTag.getEntityTagAccGrantsOrganizations().stream()
                .map(EntityTagAccGrantsOrganization::getOrganizationId)).collect(
            Collectors.toList());

        List<UUID> userIds2 = orSearchAllEntityTagsPaged.stream().flatMap(
            entityTag -> entityTag.getEntityTagAccGrantsUsers().stream()
                .map(EntityTagAccGrantsUser::getUserSid)).collect(
            Collectors.toList());

        List<UUID> ownerIdsFound = orSearchAllEntityTagsPaged.stream()
            .flatMap(entityTag -> entityTag.getOwners().stream().map(
                EntityTagOwnership::getUserSid)).collect(
                Collectors.toList());

        Set<Organization> orgGrants2 = organizationRepository.findByIdentifiers(orgIds2);
        Set<User> userGrants2 = userRepository.findBySidIn(userIds2);
        Set<User> owners2 = userRepository.findBySidIn(ownerIdsFound);

        return ResponseEntity.ok(new PageImpl<>(tags.stream().map(entityTag ->
            EntityTagEventFactory.getEntityTagEventWithGrantData(entityTag, orgGrants2, userGrants2,
                currentUser, owners2)
        ).collect(Collectors.toList()), pageable, orSearchAllEntityTagsPaged.getTotalElements()));
    }
  }

  @GetMapping(value = "/dataAssociated/{hierarchyIdentifier}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<TagResponse> getDataAssociatedEntityTags(
      @PathVariable String hierarchyIdentifier) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(entityTagService.getAllAggregateEntityTagsAssociatedToData(hierarchyIdentifier));

  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Setter
  @Getter
  public static class TagResponse {

    private List<EntityTagResponse> entityTagResponses;

    private List<ComplexTagDto> complexTagDtos;
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
//    keycloakService.generateTokenForUser(token);
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

    List<ComplexTagDto> collect = entityTagService.getComplexTagDtos(
        allComplexTags);

    return ResponseEntity.ok(collect);
  }


  @PostMapping("/complex")
  public ResponseEntity<ComplexTagDto> getComplexTag(@RequestBody ComplexTagDto request) {
    ComplexTagOwnership complexTagOwnership = ComplexTagOwnership.builder()
        .userSid(userService.getCurrentUser().getSid())
        .build();

    ComplexTag complexTag1 = ComplexTag.builder()
        .formula(request.getFormula())
        .hierarchyId(request.getHierarchyId())
        .hierarchyType(request.getHierarchyType())
        .tagName(request.getTagName())
        .tags(request.getTags())
        .build();
    complexTagOwnership.setComplexTag(complexTag1);
    complexTag1.setOwners(List.of(complexTagOwnership));
    ComplexTag complexTag = entityTagService.saveComplexTag(complexTag1);
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

  @PostMapping("/complex/delete")
  public ResponseEntity<?> deleteComplexTag(@RequestBody ComplexTagToDelete tagToDelete) {
    entityTagService.deleteComplexTag(tagToDelete);
    return ResponseEntity.accepted()
        .body("tag id: " + tagToDelete.id + " name: " + tagToDelete.getTag() + "deleted");
  }

  @PostMapping("/delete")
  public ResponseEntity<?> deleteSimpleTags(@RequestBody List<SimpleTagToDelete> tagsToDelete)
      throws IOException {
    entityTagService.deleteSimpleTags(tagsToDelete);
    return ResponseEntity.accepted().build();
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Setter
  @Getter
  public static class ComplexTagToDelete implements Serializable {

    private Integer id;
    private String type;
    private String tag;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Setter
  @Getter
  public static class SimpleTagToDelete implements Serializable {

    private UUID id;
    private String type;
    private String tag;
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

    return ResponseEntity.ok(entityTagService.getComplexTagDtos(entityTags));

  }

  @PostMapping("/removeGrants")
  public ResponseEntity<?> removeGrants(
      @RequestBody List<EntityTagRequest> request) {

    List<String> usernames = request.stream()
        .filter(entityTagRequest -> entityTagRequest.getResultingUsers() != null).flatMap(
            entityTagRequest -> entityTagRequest.getResultingUsers().stream()
                .map(UserGrant::getUsername)).collect(
            Collectors.toList());

    List<String> orgNames = request.stream()
        .filter(entityTagRequest -> entityTagRequest.getResultingOrgs() != null).flatMap(
            entityTagRequest -> entityTagRequest.getResultingOrgs().stream()
                .map(OrgGrant::getName)).collect(
            Collectors.toList());

    Map<String, User> usersByUsername = userRepository.findByUsernameIn(usernames).stream()
        .collect(Collectors.toMap(User::getUsername, user -> user, (a, b) -> b));

    Map<String, Organization> orgsByName = organizationRepository.findByNameIn(orgNames).stream()
        .collect(
            Collectors.toMap(Organization::getName, organization -> organization, (a, b) -> b));

    request.stream()
        .filter(entityTagRequest -> entityTagRequest.getResultingUsers() != null)
        .forEach(entityTagRequest -> entityTagService.deleteUserTagAccessGrants(
            UUID.fromString(entityTagRequest.getIdentifier()),
            entityTagRequest.getResultingUsers().stream()
                .map(userGrant -> usersByUsername.get(userGrant.getUsername()).getSid()).collect(
                    Collectors.toList())));

    request.stream().filter(entityTagRequest -> entityTagRequest.getResultingOrgs() != null)
        .forEach(entityTagRequest -> entityTagService.deleteOrgTagAccessGrants(
            UUID.fromString(entityTagRequest.getIdentifier()),
            entityTagRequest.getResultingOrgs().stream()
                .map(orgGrant -> orgsByName.get(orgGrant.getName()).getIdentifier()).collect(
                    Collectors.toList())));

    return ResponseEntity.accepted().build();

  }

  @PostMapping("/removeComplexTagGrants")
  public ResponseEntity<?> removeComplexTagGrants(
      @RequestBody List<ComplexTagDto> request) {

    List<String> usernames = request.stream()
        .filter(entityTagRequest -> entityTagRequest.getResultingUsers() != null).flatMap(
            entityTagRequest -> entityTagRequest.getResultingUsers().stream()
                .map(UserGrant::getUsername)).collect(
            Collectors.toList());

    List<String> orgNames = request.stream()
        .filter(entityTagRequest -> entityTagRequest.getResultingOrgs() != null).flatMap(
            entityTagRequest -> entityTagRequest.getResultingOrgs().stream()
                .map(OrgGrant::getName)).collect(
            Collectors.toList());

    Map<String, User> usersByUsername = userRepository.findByUsernameIn(usernames).stream()
        .collect(Collectors.toMap(User::getUsername, user -> user, (a, b) -> b));

    Map<String, Organization> orgsByName = organizationRepository.findByNameIn(orgNames).stream()
        .collect(
            Collectors.toMap(Organization::getName, organization -> organization, (a, b) -> b));

    request.stream()
        .filter(complexTagDto -> complexTagDto.getResultingUsers() != null)
        .forEach(complexTagDto -> entityTagService.deleteUserComplexTagAccessGrants(
            complexTagDto.getId(),
            complexTagDto.getResultingUsers().stream()
                .map(userGrant -> usersByUsername.get(userGrant.getUsername()).getSid()).collect(
                    Collectors.toList())));

    request.stream().filter(complexTagDto -> complexTagDto.getResultingOrgs() != null)
        .forEach(complexTagDto -> entityTagService.deleteOrgComplexTagAccessGrants(
            complexTagDto.getId(),
            complexTagDto.getResultingOrgs().stream()
                .map(orgGrant -> orgsByName.get(orgGrant.getName()).getIdentifier()).collect(
                    Collectors.toList())));

    return ResponseEntity.accepted().build();
  }


}


