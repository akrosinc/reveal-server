package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagFactory;
import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagResponseFactory;
import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;
import com.revealprecision.revealserver.api.v1.dto.response.EntityTagResponse;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.EntityTag.Fields;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import com.revealprecision.revealserver.persistence.repository.EntityTagRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EntityTagService {

  private final EntityTagRepository entityTagRepository;
  private final LookupEntityTypeService lookupEntityTypeService;

  public Page<EntityTag> getAllEntityTags(Pageable pageable) {
    return entityTagRepository.findAll(pageable);
  }

  public List<EntityTag> getEntityTagsByLookupEntityTypeIdentifier(
      UUID lookupEntityTypeIdentifierUuid) {
    return new ArrayList<>(
        entityTagRepository.findByLookupEntityType_Identifier(lookupEntityTypeIdentifierUuid));
  }

  public Optional<EntityTag> getEntityTagByTagName(String name) {
    return entityTagRepository.getFirstByTag(name);
  }

  public EntityTag getEntityTagByIdentifier(UUID identifier) {
    return entityTagRepository.findById(identifier).orElseThrow(() -> new NotFoundException(
        Pair.of(Fields.identifier, identifier), EntityTag.class));
  }

  public EntityTag createEntityTag(EntityTagRequest entityTagRequest) {

    LookupEntityType lookupEntityType = lookupEntityTypeService.getLookupEntityTypeByCode(
        entityTagRequest.getEntityType().getLookupEntityType());

    return entityTagRepository.save(EntityTagFactory.toEntity(entityTagRequest, lookupEntityType));
  }

  public List<EntityTagResponse> getTagsAndCoreFields(UUID lookupEntityTypeIdentifier) {
    LookupEntityType lookupEntityType = lookupEntityTypeService.getLookUpEntityTypeById(lookupEntityTypeIdentifier);
    List<EntityTagResponse> response = new ArrayList<>();

    lookupEntityType.getEntityTags().forEach(entityTag -> {
      response.add(EntityTagResponseFactory.fromEntity(entityTag));
    });

    lookupEntityType.getCoreFields().forEach(coreField -> {
      response.add(EntityTagResponseFactory.fromCoreField(coreField));
    });
    return response;
  }

}
