package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.api.v1.dto.request.ActionTemplateRequest;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.ActionTemplate;
import com.revealprecision.revealserver.persistence.repository.ActionTemplateRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActionTemplateService {

  private final ActionTemplateRepository actionTemplateRepository;

  @Transactional
  public ActionTemplate create(ActionTemplateRequest request) {
    ActionTemplate actionTemplate = ActionTemplate.builder()
        .name(request.getName())
        .actionTemplateConfig(request.getActionTemplateConfig())
        .build();
    return actionTemplateRepository.save(actionTemplate);
  }

  public Page<ActionTemplate> getAll(Pageable pageable) {
    return actionTemplateRepository.findAll(pageable);
  }

  public ActionTemplate findById(UUID identifier) {
    return actionTemplateRepository.findById(identifier)
        .orElseThrow(
            () -> new NotFoundException(Pair.of("identifier", identifier), ActionTemplate.class));
  }

  @Transactional
  public ActionTemplate update(UUID identifier, ActionTemplateRequest request) {
    ActionTemplate actionTemplate = findById(identifier);
    actionTemplate.setName(request.getName());
    actionTemplate.setActionTemplateConfig(request.getActionTemplateConfig());
    return actionTemplateRepository.save(actionTemplate);
  }

  @Transactional
  public void delete(UUID identifier) {
    ActionTemplate actionTemplate = findById(identifier);
    actionTemplateRepository.delete(actionTemplate);
  }
}
