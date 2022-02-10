package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.request.EffectivePeriod;
import com.revealprecision.revealserver.api.v1.dto.response.ActionResponse;
import com.revealprecision.revealserver.persistence.domain.Action;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionResponseFactory {

  public static ActionResponse fromEntity(Action action) {
    return ActionResponse.builder()
        .identifier(action.getIdentifier())
        .title(action.getTitle())
        .description(action.getDescription())
        .timingPeriod(EffectivePeriod.builder()
            .start(action.getTimingPeriodStart())
            .end(action.getTimingPeriodEnd())
            .build())
        .type(action.getType())
        .formIdentifier(action.getForm().getIdentifier())
        .build();
  }

  public static Page<ActionResponse> fromEntityPage(Page<Action> actions, Pageable pageable) {
    var response = actions.getContent().stream()
        .map(ActionResponseFactory::fromEntity)
        .collect(Collectors.toList());
    return new PageImpl<>(response, pageable, actions.getTotalElements());
  }
}
