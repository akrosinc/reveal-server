package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.persistence.domain.EntityTags;
import com.revealprecision.revealserver.persistence.repository.EntityTagRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EntityTagService {

  private final EntityTagRepository entityTagRepository;

  public EntityTagService(EntityTagRepository entityTagRepository){
    this.entityTagRepository = entityTagRepository;
  }

  public List<EntityTags> getAllEntityTags(){
    return entityTagRepository.findAll();
  }
}
