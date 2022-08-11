package com.revealprecision.revealserver.service;

import static com.revealprecision.revealserver.constants.EntityTagDataAggregationMethods.AVERAGE_;
import static com.revealprecision.revealserver.constants.EntityTagDataAggregationMethods.COUNT_;
import static com.revealprecision.revealserver.constants.EntityTagDataAggregationMethods.MAX_;
import static com.revealprecision.revealserver.constants.EntityTagDataAggregationMethods.MIN_;
import static com.revealprecision.revealserver.constants.EntityTagDataAggregationMethods.SUM_;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.BOOLEAN;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.DOUBLE;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.INTEGER;
import static com.revealprecision.revealserver.constants.EntityTagDataTypes.STRING;

import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagEventFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagRequestFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;
import com.revealprecision.revealserver.api.v1.dto.response.EntityTagResponse;
import com.revealprecision.revealserver.constants.EntityTagScopes;
import com.revealprecision.revealserver.enums.LookupEntityTypeCodeEnum;
import com.revealprecision.revealserver.exceptions.DuplicateCreationException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.messaging.message.EntityTagEvent;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.FormField;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import com.revealprecision.revealserver.persistence.repository.EntityTagRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  private final LookupEntityTypeService lookupEntityTypeService;
  private final FormFieldService formFieldService;

  private static final Map<String, List<String>> aggregationMethods = Map.of(
      INTEGER, List.of(SUM_, MAX_, MIN_, AVERAGE_),
      DOUBLE, List.of(SUM_, MAX_, MIN_, AVERAGE_),
      STRING, List.of(COUNT_),
      BOOLEAN, List.of(COUNT_));

  public Page<EntityTag> getAllPagedEntityTags(Pageable pageable) {
    return entityTagRepository.findAll(pageable);
  }

  public Page<EntityTag> getAllPagedGlobalNonAggregateEntityTags(Pageable pageable) {
    return entityTagRepository.findEntityTagsByScopeAndIsAggregate(EntityTagScopes.GLOBAL, false,
        pageable);
  }

  public Page<EntityTag> getAllPagedGlobalEntityTags(Pageable pageable) {
    return entityTagRepository.findEntityTagsByScope(EntityTagScopes.GLOBAL, pageable);
  }


  public List<EntityTag> getAllGlobalEntityTagsByLookupEntityTypeIdentifier(
      UUID identifier) {
    return entityTagRepository.findEntityTagsByScopeAndLookupEntityType_Identifier(
        EntityTagScopes.GLOBAL, identifier);
  }

  public List<EntityTag> getAllGlobalNonAggregateEntityTagsByLookupEntityTypeIdentifier(
       UUID identifier) {
    return entityTagRepository.findEntityTagsByScopeAndIsAggregateAndLookupEntityType_Identifier(
        EntityTagScopes.GLOBAL, false, identifier);
  }

  public List<EntityTag> getEntityTagsByLookupEntityTypeIdentifier(
      UUID lookupEntityTypeIdentifierUuid) {
    return new ArrayList<>(
        entityTagRepository.findByLookupEntityType_Identifier(lookupEntityTypeIdentifierUuid));
  }



  public Optional<EntityTag> getEntityTagByTagName(String name) {
    return entityTagRepository.getFirstByTag(name);
  }

  public Set<EntityTag> getEntityTagsByTagNames(Set<String> names) {
    return entityTagRepository.findEntityTagsByTagIn(names);
  }

  public Optional<EntityTag> getEntityTagByTagNameAndLookupEntityType(String name,
      LookupEntityTypeCodeEnum typeCodeEnum) {

    return entityTagRepository.findEntityTagsByTagAndLookupEntityType_Code(name,
        typeCodeEnum.getLookupEntityType());
  }

  public EntityTag getEntityTagByIdentifier(UUID identifier) {
    return entityTagRepository.findById(identifier).orElseThrow(() -> new NotFoundException(
        Pair.of(EntityTag.Fields.identifier, identifier), EntityTag.class));
  }

  public EntityTag createEntityTag(EntityTagRequest entityTagRequest) {

    LookupEntityType lookupEntityType = lookupEntityTypeService.getLookupEntityTypeByCode(
        entityTagRequest.getEntityType().getLookupEntityType());
    Set<FormField> formFields = null;
    if (entityTagRequest.getFormFieldNames() != null) {
      formFields = entityTagRequest.getFormFieldNames().entrySet().stream()
          .map(entry -> formFieldService.findByNameAndFormTitle(
              entry.getValue(), entry.getKey()))
          .filter(Objects::nonNull).collect(Collectors.toSet());
    }
    Optional<EntityTag> entityTagsByTagAndLookupEntityType_code = getEntityTagByTagNameAndLookupEntityType(
        entityTagRequest.getTag(), LookupEntityTypeCodeEnum.lookup(lookupEntityType.getCode()));
    if (entityTagsByTagAndLookupEntityType_code.isPresent()) {
      throw new DuplicateCreationException(
          "Entity tag with name " + entityTagRequest.getTag() + " for entity type "
              + lookupEntityType.getCode() + " already exists");
    }
    EntityTag save = entityTagRepository.save(
        EntityTagFactory.toEntity(entityTagRequest, lookupEntityType, formFields));

    Set<FormField> finalFormFields = formFields;
    List<EntityTagEvent> entityTagEvents = aggregationMethods.get(save.getValueType()).stream()
        .map(aggregationMethod ->
            createAggregateEntityTag(entityTagRequest, aggregationMethod, lookupEntityType,
                finalFormFields, true)).map(EntityTagEventFactory::getEntityTagEvent)
        .collect(Collectors.toList());

    log.debug("Automatically Created {} for requested tag creation: {}", entityTagEvents,
        entityTagRequest);
    return save;
  }

  private EntityTag createAggregateEntityTag(EntityTagRequest entityTagRequest, String str,
      LookupEntityType lookupEntityType, Set<FormField> formFields, boolean isAggregate) {
    EntityTagRequest entityTagSum = EntityTagRequestFactory.getCopy(entityTagRequest);
    entityTagSum.setTag(entityTagSum.getTag().concat(str));
    entityTagSum.setAggregate(isAggregate);
    return entityTagRepository.save(
        EntityTagFactory.toEntity(entityTagSum, lookupEntityType, formFields));
  }

  public Set<EntityTag> findEntityTagsByFormField(FormField formField) {

    return entityTagRepository.findEntityTagsByFormFields(formField);
  }

  public Optional<EntityTag> findEntityTagById(UUID entityTagIdentifier) {
    return entityTagRepository.findById(entityTagIdentifier);
  }


  public List<EntityTag> findEntityTagsByIdList(Set<UUID> entityTagIdentifiers) {
    return entityTagRepository.findEntityTagsByIdentifierIn(entityTagIdentifiers);
  }

  public Set<EntityTag> findEntityTagsByReferencedTags(String name) {
    return entityTagRepository.findEntityTagByReferencedFields(name);
  }

  public List<EntityTagResponse> getTagsAndCoreFields(UUID lookupEntityTypeIdentifier) {

    LookupEntityType lookupEntityType = lookupEntityTypeService.getLookUpEntityTypeById(
        lookupEntityTypeIdentifier);

    List<EntityTagResponse> response = lookupEntityType.getEntityTags().stream()
        .filter(EntityTag::isAddToMetadata)
        .map(EntityTagResponseFactory::fromEntity).collect(Collectors.toList());

    response.addAll(
        lookupEntityType.getCoreFields().stream().map(EntityTagResponseFactory::fromCoreField)
            .collect(Collectors.toList()));
    return response;
  }

}
