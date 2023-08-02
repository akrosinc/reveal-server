package com.revealprecision.revealserver.service;

import static com.revealprecision.revealserver.constants.EntityTagDataAggregationMethods.AVERAGE_;
import static com.revealprecision.revealserver.constants.EntityTagDataAggregationMethods.MAX_;
import static com.revealprecision.revealserver.constants.EntityTagDataAggregationMethods.MEDIAN_;
import static com.revealprecision.revealserver.constants.EntityTagDataAggregationMethods.MIN_;
import static com.revealprecision.revealserver.constants.EntityTagDataAggregationMethods.SUM_;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.DOUBLE;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.INTEGER;
import static com.revealprecision.revealserver.service.SimulationHierarchyService.GENERATED;

import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagEventFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagRequestFactory;
import com.revealprecision.revealserver.api.v1.dto.request.EntityTagItem;
import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;
import com.revealprecision.revealserver.api.v1.dto.request.UpdateEntityTagRequest;
import com.revealprecision.revealserver.api.v1.dto.response.EntityTagResponse;
import com.revealprecision.revealserver.constants.EntityTagFieldTypes;
import com.revealprecision.revealserver.exceptions.DuplicateCreationException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.User.Fields;
import com.revealprecision.revealserver.persistence.repository.EntityTagRepository;
import com.revealprecision.revealserver.persistence.repository.GeneratedHierarchyMetadataRepository;
import com.revealprecision.revealserver.persistence.repository.ImportAggregateRepository;
import com.revealprecision.revealserver.persistence.repository.ResourceAggregateRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntityTagService {

  private final EntityTagRepository entityTagRepository;

  private final ImportAggregateRepository importAggregateRepository;
  private final ResourceAggregateRepository resourceAggregateRepository;
  private final GeneratedHierarchyMetadataRepository generatedHierarchyMetadataRepository;


  public static final Map<String, List<String>> aggregationMethods = Map.of(
      INTEGER, List.of(SUM_, MAX_, MIN_, AVERAGE_,MEDIAN_),
      DOUBLE, List.of(SUM_, MAX_, MIN_, AVERAGE_,MEDIAN_));

  public List<EntityTag> getAllEntityTags() {
    return entityTagRepository.findAll();
  }


  public Page<EntityTag> getOrSearchAllEntityTagsPaged(Pageable pageable, String search) {
    return entityTagRepository.findOrSearchEntityTags(pageable, search);
  }

  public Page<EntityTag> getAllPagedNonAggregateEntityTags(Pageable pageable, String search) {
    return entityTagRepository.findEntityTagsIsAggregate(false,
        pageable, search);
  }

  public List<EntityTag> getAllAggregateEntityTags() {
    return entityTagRepository.findEntityTagsByIsAggregate(
        true);
  }

  public List<EntityTag> getAllNonAggregateEntityTags() {
    return entityTagRepository.findEntityTagsByIsAggregate(
        false);
  }

  public List<EntityTagResponse> getAllAggregateEntityTagsAssociatedToData(String hierarchyIdentifier) {

    List<EntityTagResponse> resourceTags = resourceAggregateRepository.getUniqueDataTagsAssociatedWithData(
        hierarchyIdentifier).stream().map(tagProjection -> EntityTagResponse.builder()
                .fieldType(EntityTagFieldTypes.RESOURCE_PLANNING)
                .isAggregate(true)
                .tag(tagProjection.getTag())
                .subType(tagProjection.getEventType())
                .valueType(DOUBLE)
                .build()


    ).collect(Collectors.toList());

    List<EntityTagResponse> importTags = importAggregateRepository.getUniqueDataTagsAssociatedWithData(
        hierarchyIdentifier).stream().map(tag ->
        EntityTagResponse.builder()
            .fieldType(EntityTagFieldTypes.IMPORT)
            .subType("Import")
            .isAggregate(true)
            .tag(tag)
            .valueType(DOUBLE)
            .build()

    ).collect(Collectors.toList());

    List<EntityTagResponse> generated = generatedHierarchyMetadataRepository.getUniqueDataTagsAssociatedWithData(
        hierarchyIdentifier).stream().map(tagProjection ->
        EntityTagResponse.builder()
            .fieldType(tagProjection.getEventType())
            .subType(GENERATED)
            .isAggregate(true)
            .tag(tagProjection.getTag())
            .valueType(DOUBLE)
            .build()

    ).collect(Collectors.toList());

    List<EntityTagResponse> allTags = new ArrayList<>();
    allTags.addAll(resourceTags);
    allTags.addAll(importTags);
    allTags.addAll(generated);

    Map<String, EntityTag> collect = entityTagRepository.findEntityTagsByTagIn(
            allTags.stream().map(EntityTagResponse::getTag).collect(Collectors.toSet())).stream()
        .collect(Collectors.toMap(EntityTag::getTag, a -> a, (a, b) -> b));

    return allTags.stream()
        .peek(allTag -> {
          allTag.setIdentifier(collect.get(allTag.getTag()).getIdentifier());
          allTag.setAggregate(collect.get(allTag.getTag()).isAggregate());
          allTag.setSimulationDisplay(collect.get(allTag.getTag()).isSimulationDisplay());
        }).collect(
            Collectors.toList());

  }

  public Page<EntityTag> getAllNonAggregateEntityTagsPaged(Pageable pageable) {
    return entityTagRepository.findEntityTagsByIsAggregate(
        false, pageable);
  }

  public Optional<EntityTag> getEntityTagByTagName(String name) {
    return entityTagRepository.getFirstByTag(name);
  }

  public Set<EntityTag> getEntityTagsByTagNames(Set<String> names) {
    return entityTagRepository.findEntityTagsByTagIn(names);
  }

  public EntityTag getEntityTagByIdentifier(UUID identifier) {
    return entityTagRepository.findById(identifier).orElseThrow(() -> new NotFoundException(
        Pair.of(EntityTag.Fields.identifier, identifier), EntityTag.class));
  }

  public List<EntityTag> createEntityTagsSkipExisting(EntityTagRequest entityTagRequest,
      boolean createAggregateTags) {

    Set<EntityTag> entityTagsByTagNamesAndLookupEntityType = getEntityTagsByTagNames(
        entityTagRequest.getTags().stream().map(EntityTagItem::getName)
            .collect(Collectors.toSet()));

    List<EntityTagRequest> tagsToSave = entityTagRequest.getTags().stream().filter(
            entityTagRequestItem -> !entityTagsByTagNamesAndLookupEntityType.stream()
                .map(EntityTag::getTag)
                .collect(Collectors.toList()).contains(entityTagRequestItem.getName()))
        .map(entity -> {
          EntityTagRequest entityTagRequest1 = EntityTagRequestFactory.getCopy(entityTagRequest);
          entityTagRequest1.setTag(entity.getName());
          return entityTagRequest1;
        })
        .collect(Collectors.toList());

    Map<String, EntityTagRequest> stringEntityTagRequestMap = tagsToSave.stream().
        collect(Collectors.toMap(EntityTagRequest::getTag, a -> a, (a, b) -> b));

    List<EntityTag> tags = tagsToSave.stream().map(
        EntityTagFactory::toEntity
    ).collect(Collectors.toList());

    List<EntityTag> entityTags = entityTagRepository.saveAll(tags);

    if (createAggregateTags) {
      List<EntityTag> autoCreatedTags = entityTags.stream().flatMap(save ->
            aggregationMethods.get(save.getValueType()) == null ? null
                : aggregationMethods.get(save.getValueType()).stream()
                    .map(aggregationMethod ->
                        createAggregateEntityTag(stringEntityTagRequestMap.get(save.getTag()),
                            aggregationMethod,
                            true))

      ).collect(Collectors.toList());

      entityTags.addAll(autoCreatedTags);
    }
    return entityTags;
  }


  public EntityTag createEntityTag(EntityTagRequest entityTagRequest, boolean createAggregateTags) {

    Optional<EntityTag> entityTagByTagName = getEntityTagByTagName(
        entityTagRequest.getTag());

    if (entityTagByTagName.isEmpty()) {
      EntityTag save = entityTagRepository.save(
          EntityTagFactory.toEntity(entityTagRequest));

      if (createAggregateTags) {
        List<EntityTagEvent> entityTagEvents =
            aggregationMethods.get(save.getValueType()) == null ? null
                : aggregationMethods.get(save.getValueType()).stream()
                    .map(aggregationMethod ->
                        createAggregateEntityTag(entityTagRequest, aggregationMethod,
                            true)).map(EntityTagEventFactory::getEntityTagEvent)
                    .collect(Collectors.toList());

        log.debug("Automatically Created {} for requested tag creation: {}", entityTagEvents,
            entityTagRequest);
      }

      return save;
    } else {
      throw new DuplicateCreationException(entityTagRequest.getTag() + " already exists");
    }
  }

  public EntityTag createAggregateEntityTag(EntityTagRequest entityTagRequest, String str,
      boolean isAggregate) {
    EntityTagRequest entityTagSum = EntityTagRequestFactory.getCopy(entityTagRequest);
    entityTagSum.setTag(entityTagSum.getTag().concat(str));
    entityTagSum.setAggregate(isAggregate);
    return entityTagRepository.save(
        EntityTagFactory.toEntity(entityTagSum));
  }


  public EntityTag updateEntityTag(UpdateEntityTagRequest tag) {

    EntityTag tag1 = entityTagRepository.findById(tag.getIdentifier())
        .orElseThrow(() -> new NotFoundException(Pair.of(Fields.identifier, tag.getIdentifier()),
            EntityTag.class));

    tag1.setSimulationDisplay(tag.isSimulationDisplay());

    return entityTagRepository.save(tag1);
  }

  public void saveEntityTags(Set<EntityTag> entityTags) {
    entityTagRepository.saveAll(entityTags);
  }

}
