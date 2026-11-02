package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.ActionTemplateResponse;
import com.revealprecision.revealserver.persistence.domain.ActionTemplate;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class ActionTemplateResponseFactory {

  private ActionTemplateResponseFactory() {
  }

  public static ActionTemplateResponse fromEntity(ActionTemplate actionTemplate) {
    if (actionTemplate == null) {
      return null;
    }
    return ActionTemplateResponse.builder()
        .identifier(actionTemplate.getIdentifier())
        .name(actionTemplate.getName())
        .actionTemplateConfig(actionTemplate.getActionTemplateConfig())
        .build();
  }

  public static Page<ActionTemplateResponse> fromEntityPage(Page<ActionTemplate> actionTemplates,
      Pageable pageable) {
    var response = actionTemplates.getContent().stream()
        .map(ActionTemplateResponseFactory::fromEntity)
        .collect(Collectors.toList());
    return new PageImpl<>(response, pageable, actionTemplates.getTotalElements());
  }
}
