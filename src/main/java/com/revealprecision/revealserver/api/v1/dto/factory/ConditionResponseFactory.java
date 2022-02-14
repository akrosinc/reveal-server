package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.ConditionResponse;
import com.revealprecision.revealserver.enums.EntityPropertiesEnum;
import com.revealprecision.revealserver.persistence.domain.Condition;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConditionResponseFactory {

  public static ConditionResponse fromEntity(Condition condition) {
    var targets = condition.getTargets()
        .stream()
        .map(TargetResponseFactory::fromEntity)
        .collect(Collectors.toSet());

    return ConditionResponse.builder()
        .identifier(condition.getIdentifier())
        .entity(EntityPropertiesEnum.PERSON)
        .filterValue("5")
        .operator("<=")
        .entityProperty("age")
        .targets(targets)
        .build();
  }

  public static Page<ConditionResponse> fromEntityPage(Page<Condition> conditions,
      Pageable pageable) {
    var response = conditions.getContent().stream()
        .map(ConditionResponseFactory::fromEntity)
        .collect(Collectors.toList());
    return new PageImpl<>(response, pageable, conditions.getTotalElements());
  }
}
