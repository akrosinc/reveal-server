package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.persistence.domain.EntityTag;
import com.revealprecision.revealserver.persistence.repository.EntityTagRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EntityTagService {

  private final EntityTagRepository entityTagRepository;

  public List<EntityTag> getAllEntityTags() {
    return entityTagRepository.findAll();
  }

  public List<EntityTag> getEntityTagsByLookupEntityTypeIdentifier(
      UUID lookupEntityTypeIdentifierUuid) {
    return new ArrayList<>(
        entityTagRepository.findByLookupEntityType_Identifier(lookupEntityTypeIdentifierUuid));
  }

  public Optional<EntityTag> getEntityTagsIdentifier(
      UUID entityTagIdentifier) {
    return entityTagRepository.findById(entityTagIdentifier);
  }

}
