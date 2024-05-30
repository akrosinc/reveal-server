package com.revealprecision.revealserver.api.v1.controller.querying;

import com.revealprecision.revealserver.api.v1.dto.factory.EntityTagRequestFactory;
import com.revealprecision.revealserver.api.v1.dto.request.EntityTagRequest;
import com.revealprecision.revealserver.persistence.domain.EntityTagAccGrantsUser;
import com.revealprecision.revealserver.persistence.repository.EntityTagRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reveal-entity-tag")
@RequiredArgsConstructor
@Slf4j
@Profile("Testing")
public class SandboxController {

  private final EntityTagRepository entityTagRepository;

  @GetMapping(name ="/tags")
  private List<EntityTagRequest> getAllTags (){
   return entityTagRepository.findAll()
        .stream().map(tag -> {
         List<EntityTagAccGrantsUser> entityTagAccGrantsUsers = tag.getEntityTagAccGrantsUsers();
         return EntityTagRequestFactory.getEntity(tag);
        }).collect(Collectors.toList());
  }
}
