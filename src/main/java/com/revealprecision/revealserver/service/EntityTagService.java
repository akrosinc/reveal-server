package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagFactory;
import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;
import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.domain.LookupEntityType;
import com.revealprecision.revealserver.persistence.repository.EntityTagRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  public EntityTag createEntityTag(EntityTagRequest entityTagRequest) {

    LookupEntityType lookupEntityType = lookupEntityTypeService.getLookupEntityTypeByCode(
        entityTagRequest.getEntityType().getLookupEntityType());

    return entityTagRepository.save(EntityTagFactory.toEntity(entityTagRequest, lookupEntityType));
  }

}
