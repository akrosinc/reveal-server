package com.revealprecision.revealserver.service;

import static com.revealprecision.revealserver.constants.EntityTagDataAggregationMethods.AVERAGE_;
import static com.revealprecision.revealserver.constants.EntityTagDataAggregationMethods.COUNT;
import static com.revealprecision.revealserver.constants.EntityTagDataAggregationMethods.MAX_;
import static com.revealprecision.revealserver.constants.EntityTagDataAggregationMethods.MEDIAN_;
import static com.revealprecision.revealserver.constants.EntityTagDataAggregationMethods.MIN_;
import static com.revealprecision.revealserver.constants.EntityTagDataAggregationMethods.SUM_;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.BOOLEAN;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.DOUBLE;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.INTEGER;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.STRING;

import com.revealprecision.revealserver.api.v1.controller.EntityTagController.ComplexTagToDelete;
import com.revealprecision.revealserver.api.v1.controller.EntityTagController.SimpleTagToDelete;
import com.revealprecision.revealserver.api.v1.controller.EntityTagController.TagResponse;
import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagEventFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagRequestFactory;
import com.revealprecision.revealserver.api.v1.dto.request.EntityTagItem;
import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;
import com.revealprecision.revealserver.api.v1.dto.request.UpdateEntityTagRequest;
import com.revealprecision.revealserver.api.v1.dto.response.ComplexTagDto;
import com.revealprecision.revealserver.api.v1.dto.response.EntityTagResponse;
import com.revealprecision.revealserver.constants.EntityTagFieldTypes;
import com.revealprecision.revealserver.exceptions.DuplicateCreationException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent.OrgGrant;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent.Owner;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent.UserGrant;
import com.revealprecision.revealserver.persistence.domain.ComplexTag;
import com.revealprecision.revealserver.persistence.domain.ComplexTagAccGrantsOrganization;
import com.revealprecision.revealserver.persistence.domain.ComplexTagAccGrantsUser;
import com.revealprecision.revealserver.persistence.domain.ComplexTagOwnership;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.EntityTagAccGrantsOrganization;
import com.revealprecision.revealserver.persistence.domain.EntityTagAccGrantsUser;
import com.revealprecision.revealserver.persistence.domain.EntityTagOwnership;
import com.revealprecision.revealserver.persistence.domain.GeographicLevel;
import com.revealprecision.revealserver.persistence.domain.Organization;
import com.revealprecision.revealserver.persistence.domain.User;
import com.revealprecision.revealserver.persistence.domain.User.Fields;
import com.revealprecision.revealserver.persistence.domain.aggregation.ImportAggregationNumeric;
import com.revealprecision.revealserver.persistence.projection.EntityTagWithGeoLevelAndEntityTypeProjection;
import com.revealprecision.revealserver.persistence.projection.EntityTagWithGeoLevelProjection;
import com.revealprecision.revealserver.persistence.repository.ComplexTagAccGrantsOrganizationRepository;
import com.revealprecision.revealserver.persistence.repository.ComplexTagAccGrantsUserRepository;
import com.revealprecision.revealserver.persistence.repository.ComplexTagRepository;
import com.revealprecision.revealserver.persistence.repository.EntityTagAccGrantsOrganizationRepository;
import com.revealprecision.revealserver.persistence.repository.EntityTagAccGrantsUserRepository;
import com.revealprecision.revealserver.persistence.repository.EntityTagOwnershipRepository;
import com.revealprecision.revealserver.persistence.repository.EntityTagRepository;
import com.revealprecision.revealserver.persistence.repository.GeneratedHierarchyMetadataRepository;
import com.revealprecision.revealserver.persistence.repository.ImportAggregateRepository;
import com.revealprecision.revealserver.persistence.repository.ImportAggregationNumericRepository;
import com.revealprecision.revealserver.persistence.repository.OrganizationRepository;
import com.revealprecision.revealserver.persistence.repository.ResourceAggregateRepository;
import com.revealprecision.revealserver.persistence.repository.UserRepository;
import com.revealprecision.revealserver.props.SecurityProperties;
import com.revealprecision.revealserver.util.UserUtils;
import java.io.IOException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.keycloak.KeycloakPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntityTagService {

  private final EntityTagRepository entityTagRepository;
  private final EntityTagAccGrantsUserRepository entityTagAccGrantsUserRepository;
  private final EntityTagAccGrantsOrganizationRepository entityTagAccGrantsOrganizationRepository;

  private final ComplexTagAccGrantsUserRepository complexTagAccGrantsUserRepository;
  private final ComplexTagAccGrantsOrganizationRepository complexTagAccGrantsOrganizationRepository;
  private final UserService userService;
  private final EntityTagOwnershipRepository entityTagOwnershipRepository;

  private final ImportAggregateRepository importAggregateRepository;
  private final ResourceAggregateRepository resourceAggregateRepository;
  private final GeneratedHierarchyMetadataRepository generatedHierarchyMetadataRepository;
  private final ComplexTagRepository complexTagRepository;
  private final OrganizationRepository organizationRepository;
  private final UserRepository userRepository;
  private final SecurityProperties securityProperties;
  private final RestHighLevelClient client;
  private final ImportAggregationNumericRepository importAggregationNumericRepository;

  @Value("${reveal.elastic.index-name}")
  String elasticIndex;

  public static final Map<String, List<String>> aggregationMethods = Map.of(INTEGER,
      List.of(SUM_, MAX_, MIN_, AVERAGE_, MEDIAN_), DOUBLE,
      List.of(SUM_, MAX_, MIN_, AVERAGE_, MEDIAN_), STRING, List.of(COUNT), BOOLEAN,
      List.of(COUNT));

  public List<EntityTag> getAllEntityTags() {
    return entityTagRepository.findAll();
  }


  public Page<EntityTag> getOrSearchAllEntityTagsPaged(Pageable pageable, String search) {
    return entityTagRepository.findOrSearchEntityTags(pageable, search);
  }

  public Page<EntityTag> getOrSearchAllEntityTagsPagedByOwners(Pageable pageable, String search,
      UUID userSid) {
    return entityTagRepository.findOrSearchEntityTagsByOwners(pageable, search, userSid);
  }

  public Page<EntityTag> getAllPagedNonAggregateEntityTags(Pageable pageable, String search) {
    return entityTagRepository.findEntityTagsIsAggregate(false, pageable, search);
  }

  public List<EntityTag> getAllAggregateEntityTags() {
    return entityTagRepository.findEntityTagsByIsAggregate(true);
  }

  public List<EntityTag> getAllNonAggregateEntityTags() {
    return entityTagRepository.findEntityTagsByIsAggregate(false);
  }

  public TagResponse getAllAggregateEntityTagsAssociatedToData(
      String hierarchyIdentifier) {


    List<EntityTagResponse> resourceTags =
        resourceAggregateRepository.getUniqueDataTagsAssociatedWithDataWithLevels(
            hierarchyIdentifier).stream()
        .collect(Collectors.groupingBy(item->
            //create a unique name for the key for the map
            item.getEventType().concat(":").concat(item.getTagName())))
            .entrySet()
            .stream().map(resourceEntityTagWithGeoLevelProjections -> EntityTagResponse.builder()
                .fieldType(EntityTagFieldTypes.RESOURCE_PLANNING).isAggregate(true)
                .tag(resourceEntityTagWithGeoLevelProjections.getKey().split(":")[1])
                .subType(resourceEntityTagWithGeoLevelProjections.getKey().split(":")[0]).valueType(DOUBLE)
                .levels(resourceEntityTagWithGeoLevelProjections.getValue().stream()
                    .map(EntityTagWithGeoLevelAndEntityTypeProjection::getGeoName).collect(
                        Collectors.toList()))
                .build()
        ).collect(Collectors.toList());


    Map<String, List<EntityTagWithGeoLevelProjection>> currentImportDataTags =
        importAggregateRepository.getUniqueDataTagsAndLevelsListAssociatedWithData(
                hierarchyIdentifier)
            .stream().collect(Collectors.groupingBy(EntityTagWithGeoLevelProjection::getTagName));

    List<EntityTagResponse> importTags = currentImportDataTags.entrySet().stream().map(entry ->
      EntityTagResponse.builder().fieldType(EntityTagFieldTypes.IMPORT).subType("Import")
          .isAggregate(true).tag(entry.getKey()).valueType(DOUBLE).levels(entry.getValue().stream()
              .map(EntityTagWithGeoLevelProjection::getGeoName).collect(
                  Collectors.toList())).build()
    ).collect(Collectors.toList());

    List<EntityTagResponse> generated = generatedHierarchyMetadataRepository.getUniqueDataTagsAndLevelsListAssociatedWithData(
            hierarchyIdentifier).stream().collect(Collectors.groupingBy(item ->
            item.getEventType().concat(":").concat(item.getTag())))
        .entrySet().stream()
        .map(generatedEntityTagWithGeoLevelProjections -> EntityTagResponse.builder()
            .fieldType(EntityTagFieldTypes.RESOURCE_PLANNING).isAggregate(true)
            .tag(generatedEntityTagWithGeoLevelProjections.getKey().split(":")[1])
            .subType(generatedEntityTagWithGeoLevelProjections.getKey().split(":")[0]).valueType(DOUBLE)
            .levels(generatedEntityTagWithGeoLevelProjections.getValue().stream()
                .map(EntityTagWithGeoLevelAndEntityTypeProjection::getGeoName).collect(
                    Collectors.toList()))
            .build())
        .collect(Collectors.toList());

    User currentUser = userService.getCurrentUser();

    Set<UUID> currentUserOrgs = currentUser.getOrganizations().stream()
        .map(Organization::getIdentifier)
        .collect(Collectors.toSet());

    List<EntityTagResponse> allTags = new ArrayList<>();
    allTags.addAll(resourceTags);
    allTags.addAll(importTags);
    allTags.addAll(generated);

    Map<String, EntityTagResponse> tagsWithAccess = entityTagRepository.findEntityTagsByTagIn(
            allTags.stream().map(EntityTagResponse::getTag).collect(Collectors.toSet()))
        .stream()
        .filter(entityTag -> checkAccess(entityTag, currentUserOrgs, currentUser))
        .map(
            entityTag -> EntityTagResponse.builder()
                .identifier(String.valueOf(entityTag.getIdentifier()))
                .isAggregate(entityTag.isAggregate())
                .simulationDisplay(entityTag.isSimulationDisplay())
                .tag(entityTag.getTag())
                .build()
        )
        .collect(Collectors.toMap(EntityTagResponse::getTag, a -> a, (a, b) -> b));

    Set<Integer> complexTagIdByTagNamesIn = complexTagRepository.findComplexTagIdByTagNamesIn(
        allTags.stream().map(EntityTagResponse::getTag).collect(Collectors.toSet()));

    Set<ComplexTag> complexTags = complexTagRepository.findComplexTagsByIdIn(
        complexTagIdByTagNamesIn);

    Map<String, ComplexTagDto> collect2 = complexTags

        .stream()
        .filter(complexTag ->
            complexTag.getTags().stream()
                .filter(tagWithFormulaSymbol -> tagsWithAccess.containsKey(
                    tagWithFormulaSymbol.getName())).count() < complexTag.getTags().size()
        )
        .filter(complexTag -> checkAccess(complexTag, currentUserOrgs, currentUser))
        .map(
            this::getComplexTagDto)
        .collect(Collectors.toMap(ComplexTagDto::getTagName, a -> a));

    List<EntityTagResponse> collect1 = allTags.stream()
        .filter(allTag -> tagsWithAccess.get(allTag.getTag()) != null)
        .peek(allTag -> {
          allTag.setIdentifier(tagsWithAccess.get(allTag.getTag()).getIdentifier());
          allTag.setAggregate(tagsWithAccess.get(allTag.getTag()).isAggregate());
          allTag.setSimulationDisplay(tagsWithAccess.get(allTag.getTag()).isSimulationDisplay());
          allTag.setLevels(allTag.getLevels());
        }).collect(Collectors.toList());

    return new TagResponse(collect1, new ArrayList<>(collect2.values()));
  }

  public List<ComplexTagDto> getComplexTagDtos(List<ComplexTag> allComplexTags) {

    User currentUser = userService.getCurrentUser();

    List<UUID> orgIds = allComplexTags.stream()
        .flatMap(complexTag -> complexTag.getComplexTagAccGrantsOrganizations().stream().map(
            ComplexTagAccGrantsOrganization::getOrganizationId)).collect(Collectors.toList());

    List<UUID> userIds = allComplexTags.stream()
        .flatMap(complexTag -> complexTag.getComplexTagAccGrantsUsers().stream().map(
            ComplexTagAccGrantsUser::getUserSid)).collect(Collectors.toList());

    userIds.addAll(
        allComplexTags.stream().flatMap(complexTag -> complexTag.getOwners().stream().map(
            ComplexTagOwnership::getUserSid)).collect(Collectors.toList()));

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

      List<Owner> owners = userGrants.stream().filter(
              user -> complexTag.getOwners().stream().map(
                  ComplexTagOwnership::getUserSid).collect(
                  Collectors.toList()).contains(user.getSid()))
          .map(user -> new Owner(user.getSid(), user.getUsername()))
          .collect(
              Collectors.toList());

      boolean isOwner = owners.stream()
          .anyMatch(owner -> owner.getId().equals(currentUser.getSid()));

      return getComplexTagDto(complexTag, orgGrantObj, userGrantObj, owners, isOwner);

    }).collect(Collectors.toList());
    return collect;
  }

  public ComplexTagDto getComplexTagDto(ComplexTag complexTag, List<OrgGrant> orgGrantObj,
      List<UserGrant> userGrantObj, List<Owner> owners, boolean isOwner) {
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
        .owners(owners)
        .isOwner(isOwner)
        .build();
  }

  public ComplexTagDto getComplexTagDto(ComplexTag complexTag) {
    return ComplexTagDto.builder()
        .hierarchyId(complexTag.getHierarchyId())
        .formula(complexTag.getFormula())
        .tags(complexTag.getTags())
        .hierarchyType(complexTag.getHierarchyType())
        .tagName(complexTag.getTagName())
        .id(String.valueOf(complexTag.getId()))
        .isPublic(complexTag.isPublic())
        .build();
  }

  public boolean checkAccess(EntityTag entityTag, Set<UUID> currentUserOrgs, User currentUser) {
    List<UUID> grantedOrgList = entityTag.getEntityTagAccGrantsOrganizations().stream()
        .map(EntityTagAccGrantsOrganization::getOrganizationId)
        .collect(Collectors.toList());

    List<UUID> hasItemList = currentUserOrgs.stream()
        .filter(grantedOrgList::contains)
        .collect(Collectors.toList());

    List<UUID> userList = entityTag.getEntityTagAccGrantsUsers().stream()
        .map(EntityTagAccGrantsUser::getUserSid).collect(
            Collectors.toList());

    List<EntityTagOwnership> ownersMatchingCurrentUser = entityTag.getOwners().stream()
        .filter(owner -> owner.getUserSid().equals(currentUser.getSid())).collect(
            Collectors.toList());

    return hasItemList.size() > 0 || userList.contains(currentUser.getSid())
        || entityTag.isPublic() || ownersMatchingCurrentUser.size() > 0
        || UserUtils.hasRole(securityProperties.getTagAccessOverride());
  }

  public boolean checkAccess(ComplexTag entityTag, Set<UUID> currentUserOrgs, User currentUser) {
    List<UUID> grantedOrgList = entityTag.getComplexTagAccGrantsOrganizations().stream()
        .map(ComplexTagAccGrantsOrganization::getOrganizationId)
        .collect(Collectors.toList());

    List<UUID> hasItemList = currentUserOrgs.stream()
        .filter(grantedOrgList::contains)
        .collect(Collectors.toList());

    List<UUID> userList = entityTag.getComplexTagAccGrantsUsers().stream()
        .map(ComplexTagAccGrantsUser::getUserSid).collect(
            Collectors.toList());

    List<ComplexTagOwnership> ownersMatchingCurrentUser = entityTag.getOwners().stream()
        .filter(owner -> owner.getUserSid().equals(currentUser.getSid())).collect(
            Collectors.toList());

    return hasItemList.size() > 0 || userList.contains(currentUser.getSid())
        || entityTag.isPublic() || ownersMatchingCurrentUser.size() > 0
        || UserUtils.hasRole(securityProperties.getTagAccessOverride());
  }

  public Page<EntityTag> getAllNonAggregateEntityTagsPaged(Pageable pageable) {
    return entityTagRepository.findEntityTagsByIsAggregate(false, pageable);
  }

  public Optional<EntityTag> getEntityTagByTagName(String name) {
    return entityTagRepository.getFirstByTag(name);
  }

  public Set<EntityTag> getEntityTagsByTagNames(Set<String> names) {
    return entityTagRepository.findEntityTagsByTagIn(names);
  }

  public List<EntityTag> findEntityTagsByIdentifierIn(List<UUID> ids) {
    return entityTagRepository.findEntityTagsByIdentifierIn(ids);
  }

  public void deleteSimpleTags(List<SimpleTagToDelete> tagsToDelete) throws IOException {
    tagsToDelete.forEach(tagToDelete -> {
      log.info("{}", tagToDelete);
    });

    List<EntityTag> entityTagsByIdentifierIn = findEntityTagsByIdentifierIn(
        tagsToDelete.stream().map(SimpleTagToDelete::getId).collect(
            Collectors.toList()));

    saveEntityTags(entityTagsByIdentifierIn.stream()
        .peek(entityTag -> entityTag.setDeleting(true)).collect(Collectors.toSet()));

    List<String> tagNamesToDelete = entityTagsByIdentifierIn.stream().map(
        EntityTag::getTag).collect(
        Collectors.toList());

    deleteFromImportAggregation(tagNamesToDelete);
    deleteMetadataByTags(entityTagsByIdentifierIn);

  }

  @Transactional
  void deleteFromImportAggregation(List<String> tagNamesToDelete) {

    List<ImportAggregationNumeric> allByByFieldCodeIn = importAggregationNumericRepository.findAllByFieldCodeIn(
        tagNamesToDelete);

    importAggregationNumericRepository.deleteAll(allByByFieldCodeIn);
  }

  public Set<EntityTag> findEntityTagsByReferencedTagIn(List<UUID> ids) {
    return entityTagRepository.findEntityTagsByReferencedTagIn(ids);
  }

  public EntityTag getEntityTagByIdentifier(UUID identifier) {
    return entityTagRepository.findById(identifier).orElseThrow(
        () -> new NotFoundException(Pair.of(EntityTag.Fields.identifier, identifier),
            EntityTag.class));
  }

  @Transactional(rollbackOn = SQLException.class)
  public List<EntityTag> createEntityTagsSkipExisting(EntityTagRequest entityTagRequest,
      boolean createAggregateTags, GeographicLevel geographicLevel) {

    Set<EntityTag> entityTagsByTagNames = getEntityTagsByTagNames(
        entityTagRequest.getTags().stream().map(EntityTagItem::getName)
            .collect(Collectors.toSet()));

    List<EntityTagRequest> tagsToSave = entityTagRequest.getTags().stream().filter(
        entityTagRequestItem -> !entityTagsByTagNames.stream()
            .map(EntityTag::getTag).collect(Collectors.toList())
            .contains(entityTagRequestItem.getName())).map(entity -> {
      EntityTagRequest entityTagRequest1 = EntityTagRequestFactory.getCopy(entityTagRequest);
      entityTagRequest1.setTag(entity.getName());
      return entityTagRequest1;
    }).collect(Collectors.toList());

    Map<String, EntityTagRequest> stringEntityTagRequestMap = tagsToSave.stream()
        .collect(Collectors.toMap(EntityTagRequest::getTag, a -> a, (a, b) -> b));

    List<EntityTag> tags = tagsToSave.stream().map(EntityTagFactory::toEntity)
        .peek(entityTag -> entityTag.setUploadGeographicLevel(geographicLevel))
        .collect(Collectors.toList());

    List<EntityTag> entityTags = entityTagRepository.saveAll(tags);
    saveEntityTagOwnership(entityTags);

    if (createAggregateTags) {
      List<EntityTag> autoCreatedTags = entityTags.stream().flatMap(
          save -> aggregationMethods.get(save.getValueType()) == null ? null
              : aggregationMethods.get(save.getValueType()).stream().map(
                  aggregationMethod -> {
                    EntityTagRequest entityTagRequest1 = stringEntityTagRequestMap.get(
                        save.getTag());
                    entityTagRequest1.setReferencedTag(save.getIdentifier());
                    return createAggregateEntityTag(
                        entityTagRequest1, aggregationMethod, true,geographicLevel);
                  })

      ).collect(Collectors.toList());
      saveEntityTagOwnership(autoCreatedTags);
      entityTags.addAll(autoCreatedTags);
    }
    return entityTags;
  }


  @Transactional(rollbackOn = SQLException.class)
  public List<EntityTag> createEntityTagsSkipExisting(EntityTagRequest entityTagRequest,
      boolean createAggregateTags) {

    Set<EntityTag> entityTagsByTagNames = getEntityTagsByTagNames(
        entityTagRequest.getTags().stream().map(EntityTagItem::getName)
            .collect(Collectors.toSet()));

    List<EntityTagRequest> tagsToSave = entityTagRequest.getTags().stream().filter(
        entityTagRequestItem -> !entityTagsByTagNames.stream()
            .map(EntityTag::getTag).collect(Collectors.toList())
            .contains(entityTagRequestItem.getName())).map(entity -> {
      EntityTagRequest entityTagRequest1 = EntityTagRequestFactory.getCopy(entityTagRequest);
      entityTagRequest1.setTag(entity.getName());
      return entityTagRequest1;
    }).collect(Collectors.toList());

    Map<String, EntityTagRequest> stringEntityTagRequestMap = tagsToSave.stream()
        .collect(Collectors.toMap(EntityTagRequest::getTag, a -> a, (a, b) -> b));

    List<EntityTag> tags = tagsToSave.stream().map(EntityTagFactory::toEntity)
        .collect(Collectors.toList());

    List<EntityTag> entityTags = entityTagRepository.saveAll(tags);
    saveEntityTagOwnership(entityTags);

    if (createAggregateTags) {
      List<EntityTag> autoCreatedTags = entityTags.stream().flatMap(
          save -> aggregationMethods.get(save.getValueType()) == null ? null
              : aggregationMethods.get(save.getValueType()).stream().map(
                  aggregationMethod -> {
                    EntityTagRequest entityTagRequest1 = stringEntityTagRequestMap.get(
                        save.getTag());
                    entityTagRequest1.setReferencedTag(save.getIdentifier());
                    return createAggregateEntityTag(
                        entityTagRequest1, aggregationMethod, true);
                  })

      ).collect(Collectors.toList());
      saveEntityTagOwnership(autoCreatedTags);
      entityTags.addAll(autoCreatedTags);
    }
    return entityTags;
  }

  @Transactional(rollbackOn = SQLException.class)
  public EntityTag createEntityTag(EntityTagRequest entityTagRequest, boolean createAggregateTags) {

    Optional<EntityTag> entityTagByTagName = getEntityTagByTagName(entityTagRequest.getTag());

    if (entityTagByTagName.isEmpty()) {
      EntityTag save = entityTagRepository.save(EntityTagFactory.toEntity(entityTagRequest));

      saveEntityTagOwnership(List.of(save));

      if (createAggregateTags) {
        List<EntityTagEvent> entityTagEvents;
        if (aggregationMethods.get(save.getValueType()) != null) {
          List<EntityTag> entityTags = aggregationMethods.get(save.getValueType()).stream()
              .map(aggregationMethod -> {
                entityTagRequest.setReferencedTag(save.getIdentifier());
                return createAggregateEntityTag(
                    entityTagRequest, aggregationMethod, true);
              }).collect(Collectors.toList());

          saveEntityTagOwnership(entityTags);

          entityTagEvents = entityTags.stream().map(EntityTagEventFactory::getEntityTagEvent)
              .collect(Collectors.toList());

          log.debug("Automatically Created {} for requested tag creation: {}", entityTagEvents,
              entityTagRequest);
        }
      }
      return save;
    } else {
      throw new DuplicateCreationException(entityTagRequest.getTag() + " already exists");
    }
  }


  private List<EntityTagOwnership> getEntityTagOwnerShipListFromEntityTags(
      List<EntityTag> entityTags) {
    Principal owner = UserUtils.getCurrentPrinciple();

    if (owner instanceof KeycloakPrincipal) {

      List<UUID> entityTagNames = entityTags.stream().map(EntityTag::getIdentifier)
          .collect(Collectors.toList());

      Map<UUID, EntityTag> collect = entityTags.stream()
          .collect(Collectors.toMap(EntityTag::getIdentifier, i -> i, (a, b) -> b));

      UUID userSid = UUID.fromString(owner.getName());
      Optional<List<EntityTagOwnership>> entityTagOwnershipsByEntityTagId = entityTagOwnershipRepository.getEntityTagOwnershipsByEntityTag_IdentifierInAndUserSid(
          entityTagNames, userSid);

      if (entityTagOwnershipsByEntityTagId.isEmpty()
          || entityTagOwnershipsByEntityTagId.get().size() <= 0) {
        return entityTags.stream().map(
            entityTag -> EntityTagOwnership.builder()
                .entityTag(collect.get(entityTag.getIdentifier()))
                .userSid(UUID.fromString(owner.getName())).build()).collect(Collectors.toList());

      } else {
        List<EntityTagOwnership> entityTagOwnerships = entityTagOwnershipsByEntityTagId.get();
        List<UUID> entityTagOwnershipTagIds = entityTagOwnerships.stream()
            .map(EntityTagOwnership::getEntityTag).map(EntityTag::getIdentifier)
            .collect(Collectors.toList());
        return entityTags.stream()
            .filter(entityTag -> !entityTagOwnershipTagIds.contains(entityTag.getIdentifier())).map(
                entityTag -> EntityTagOwnership.builder().entityTag(entityTag)
                    .userSid(UUID.fromString(owner.getName())).build())
            .collect(Collectors.toList());
      }
    }
    return null;
  }

  private void saveEntityTagOwnership(List<EntityTag> entityTags) {

    List<EntityTagOwnership> entityTagOwnerShipListFromEntityTags = getEntityTagOwnerShipListFromEntityTags(
        entityTags);

    if (entityTagOwnerShipListFromEntityTags != null
        && entityTagOwnerShipListFromEntityTags.size() > 0) {
      entityTagOwnershipRepository.saveAll(entityTagOwnerShipListFromEntityTags);
    }

  }

  public EntityTag createAggregateEntityTag(EntityTagRequest entityTagRequest, String str,
      boolean isAggregate) {
    EntityTagRequest entityTagSum = EntityTagRequestFactory.getCopy(entityTagRequest);
    entityTagSum.setTag(entityTagSum.getTag().concat(str));
    entityTagSum.setAggregate(isAggregate);
    return entityTagRepository.save(EntityTagFactory.toEntity(entityTagSum));
  }

  public EntityTag createAggregateEntityTag(EntityTagRequest entityTagRequest, String str,
      boolean isAggregate, GeographicLevel geographicLevel) {
    EntityTagRequest entityTagSum = EntityTagRequestFactory.getCopy(entityTagRequest);
    entityTagSum.setTag(entityTagSum.getTag().concat(str));
    entityTagSum.setAggregate(isAggregate);
    EntityTag entity = EntityTagFactory.toEntity(entityTagSum);
    entity.setUploadGeographicLevel(geographicLevel);
    return entityTagRepository.save(entity);
  }


  public EntityTag updateEntityTag(UpdateEntityTagRequest tag) {

    EntityTag tag1 = entityTagRepository.findById(tag.getIdentifier()).orElseThrow(
        () -> new NotFoundException(Pair.of(Fields.identifier, tag.getIdentifier()),
            EntityTag.class));

    tag1.setSimulationDisplay(tag.isSimulationDisplay());

    return entityTagRepository.save(tag1);
  }

  public List<ComplexTag> updateComplexTagAccessGrants(List<ComplexTagDto> tags) {

    List<String> ids = tags.stream().map(ComplexTagDto::getId)
        .collect(Collectors.toList());

    Map<String, ComplexTagDto> tagMap = tags.stream()
        .collect(
            Collectors.toMap(ComplexTagDto::getId, tag -> tag, (a, b) -> a));

    Set<Integer> collect = ids.stream().map(id -> {
      try {
        return Integer.valueOf(id);
      } catch (ClassCastException e) {
        return null;
      }
    }).filter(Objects::nonNull).collect(Collectors.toSet());

    Set<ComplexTag> complexTags = complexTagRepository.findComplexTagsByIdIn(collect);

    List<ComplexTag> complexTagsToSave = complexTags.stream().map(tag -> {
      ComplexTagDto complexTagDto = tagMap.get(String.valueOf(tag.getId()));
      tag.setPublic(complexTagDto.isPublic());

      if (complexTagDto.getResultingOrgs() != null) {
        List<ComplexTagAccGrantsOrganization> orgs = complexTagDto.getResultingOrgs()
            .stream()
            .filter(org -> !tag.getComplexTagAccGrantsOrganizations().stream()
                .map(ComplexTagAccGrantsOrganization::getOrganizationId).collect(
                    Collectors.toList()).contains(org.getId()))
            .map(org -> ComplexTagAccGrantsOrganization.builder()
                .id(null)
                .complexTag(tag)
                .organizationId(org.getId())
                .build()).collect(Collectors.toList());

        tag.setComplexTagAccGrantsOrganizations(orgs);
      }

      if (complexTagDto.getResultingUsers() != null) {
        List<ComplexTagAccGrantsUser> users = complexTagDto.getResultingUsers()
            .stream()
            .filter(user -> !tag.getComplexTagAccGrantsUsers().stream()
                .map(ComplexTagAccGrantsUser::getUserSid).collect(
                    Collectors.toList()).contains(user.getId()))
            .map(user -> ComplexTagAccGrantsUser.builder()
                .userSid(user.getId())
                .complexTag(tag)
                .build()
            ).collect(Collectors.toList());

        tag.setComplexTagAccGrantsUsers(users);
      }

      return tag;
    }).collect(Collectors.toList());

    return complexTagRepository.saveAll(complexTagsToSave);
  }

  public Set<UUID> getTagsAccess(User currentUser, Set<UUID> currentUserOrgs,
      List<EntityTag> entityTagsByIdentifierIn) {
    return entityTagsByIdentifierIn.stream()
        .filter(entityTag -> {
          List<UUID> grantedOrgList = entityTag.getEntityTagAccGrantsOrganizations().stream()
              .map(EntityTagAccGrantsOrganization::getOrganizationId)
              .collect(Collectors.toList());

          List<UUID> hasItemList = currentUserOrgs.stream()
              .filter(grantedOrgList::contains)
              .collect(Collectors.toList());

          List<UUID> userList = entityTag.getEntityTagAccGrantsUsers().stream()
              .map(EntityTagAccGrantsUser::getUserSid).collect(
                  Collectors.toList());

          return hasItemList.size() > 0 || userList.contains(currentUser.getSid())
              || entityTag.isPublic();
        }).map(EntityTag::getIdentifier).collect(Collectors.toSet());
  }

  public void deleteComplexTag(ComplexTagToDelete tagToDelete) {

    Optional<ComplexTag> byId = complexTagRepository.findById(tagToDelete.getId());
    if (byId.isPresent()) {
      complexTagRepository.delete(byId.get());
    } else {
      throw new NotFoundException(
          "Complex Tag with id: " + tagToDelete.getId() + " name: " + tagToDelete.getTag()
              + " not found");
    }

  }

  public List<EntityTag> updateEntityTagAccessGrants(List<EntityTagRequest> tags) {

    List<UUID> ids = tags.stream().map(EntityTagRequest::getIdentifier).map(UUID::fromString)
        .collect(Collectors.toList());

    Map<UUID, EntityTagRequest> tagMap = tags.stream()
        .collect(
            Collectors.toMap(tag -> UUID.fromString(tag.getIdentifier()), tag -> tag, (a, b) -> a));

    List<EntityTag> entityTags = entityTagRepository.findEntityTagsByIdentifierIn(ids);

    List<EntityTag> entityTagsToSave = entityTags.stream().map(tag -> {
      EntityTagRequest entityTagRequest = tagMap.get(tag.getIdentifier());
      tag.setPublic(entityTagRequest.isPublic());

      if (entityTagRequest.getResultingOrgs() != null) {
        List<EntityTagAccGrantsOrganization> orgs = entityTagRequest.getResultingOrgs()
            .stream()
            .filter(org -> !tag.getEntityTagAccGrantsOrganizations().stream()
                .map(EntityTagAccGrantsOrganization::getOrganizationId).collect(
                    Collectors.toList()).contains(org.getId()))
            .map(org -> EntityTagAccGrantsOrganization.builder()
                .id(null)
                .entityTag(tag)
                .organizationId(org.getId())
                .build()).collect(Collectors.toList());

        tag.setEntityTagAccGrantsOrganizations(orgs);
      }

      if (entityTagRequest.getResultingUsers() != null) {
        List<EntityTagAccGrantsUser> users = entityTagRequest.getResultingUsers()
            .stream()
            .filter(user -> !tag.getEntityTagAccGrantsUsers().stream()
                .map(EntityTagAccGrantsUser::getUserSid).collect(
                    Collectors.toList()).contains(user.getId()))
            .map(user -> EntityTagAccGrantsUser.builder()
                .userSid(user.getId())
                .entityTag(tag)
                .build()
            ).collect(Collectors.toList());

        tag.setEntityTagAccGrantsUsers(users);
      }

      return tag;
    }).collect(Collectors.toList());

    return entityTagRepository.saveAll(entityTagsToSave);
  }

  public void saveEntityTags(Set<EntityTag> entityTags) {
    entityTagRepository.saveAll(entityTags);
  }

  @Transactional
  public void deleteUserTagAccessGrants(UUID entityTagIdentifier, List<UUID> uuids) {

    List<EntityTagAccGrantsUser> allByEntityTag_identifierAndUserSidIn = entityTagAccGrantsUserRepository.findAllByEntityTag_IdentifierAndUserSidIn(
        entityTagIdentifier, uuids);

    entityTagAccGrantsUserRepository.deleteAllById(
        allByEntityTag_identifierAndUserSidIn.stream().map(
            EntityTagAccGrantsUser::getId).collect(
            Collectors.toList()));
  }

  @Transactional
  public void deleteUserComplexTagAccessGrants(String complexTagId, List<UUID> uuids) {

    List<ComplexTagAccGrantsUser> allByEntityTag_identifierAndUserSidIn = complexTagAccGrantsUserRepository.findAllByComplexTag_IdAndAndUserSidIn(
        Integer.valueOf(complexTagId), uuids);

    complexTagAccGrantsUserRepository.deleteAllById(
        allByEntityTag_identifierAndUserSidIn.stream().map(
            ComplexTagAccGrantsUser::getId).collect(
            Collectors.toList()));
  }


  @Transactional
  public void deleteOrgTagAccessGrants(UUID entityTagIdentifier, List<UUID> uuids) {

    List<EntityTagAccGrantsOrganization> allByEntityTagIdentifierAndOrganizationIdIn = entityTagAccGrantsOrganizationRepository.findAllByEntityTag_IdentifierAndOrganizationIdIn(
        entityTagIdentifier, uuids);

    entityTagAccGrantsOrganizationRepository.deleteAllById(
        allByEntityTagIdentifierAndOrganizationIdIn.stream().map(
            EntityTagAccGrantsOrganization::getId).collect(
            Collectors.toList()));
  }

  @Transactional
  public void deleteOrgComplexTagAccessGrants(String entityTagIdentifier, List<UUID> uuids) {

    List<ComplexTagAccGrantsOrganization> allByEntityTagIdentifierAndOrganizationIdIn = complexTagAccGrantsOrganizationRepository.findAllByComplexTag_IdAndOrganizationIdIn(
        Integer.valueOf(entityTagIdentifier), uuids);

    complexTagAccGrantsOrganizationRepository.deleteAllById(
        allByEntityTagIdentifierAndOrganizationIdIn.stream().map(
            ComplexTagAccGrantsOrganization::getId).collect(
            Collectors.toList()));
  }


  @Async
  public void deleteMetadataByTags(List<EntityTag> tagsToDelete) throws IOException {

    List<String> tags = tagsToDelete.stream().map(
        EntityTag::getTag).collect(
        Collectors.toList());

    Map<String, Object> tagsToRemove = new HashMap<>();
    tagsToRemove.put("tagsToRemove", tags);

    UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest(elasticIndex);
    updateByQueryRequest.setScript(new Script(ScriptType.INLINE, "painless",

        "List a = ctx._source['metadata'];"
            + "List b = params.tagsToRemove; "
            + "List c = a.stream().filter(val -> val.containsKey('tag') && !b.contains(val.get('tag'))).collect(Collectors.toList());"
            + "ctx._source['metadata'] = c", tagsToRemove));

    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
    boolQuery.should().addAll(tags.stream().map(tag ->
        QueryBuilders.nestedQuery("metadata", QueryBuilders.matchPhraseQuery("metadata.tag", tag),
            ScoreMode.None)
    ).collect(Collectors.toList()));

    updateByQueryRequest.setQuery(boolQuery);

    BulkByScrollResponse bulkByScrollResponse = client.updateByQuery(updateByQueryRequest,
        RequestOptions.DEFAULT);

    entityTagRepository.deleteAllById(tagsToDelete.stream().map(EntityTag::getIdentifier).collect(
        Collectors.toList()));
  }

  public List<EntityTag> findEntityTagsByMetadataImport(UUID id) {
    return entityTagRepository.findEntityTagsByMetadataImport_Identifier(id);
  }

  public List<ComplexTag> getAllComplexTags() {
    return complexTagRepository.findAll();
  }

  public ComplexTag saveComplexTag(ComplexTag complexTag) {
    return complexTagRepository.save(complexTag);
  }

}
