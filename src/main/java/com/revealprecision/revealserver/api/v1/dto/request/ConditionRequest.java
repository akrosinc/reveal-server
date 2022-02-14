package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.EntityPropertiesEnum;
import java.util.Set;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConditionRequest {

  private EntityPropertiesEnum entity; //TODO: to be modified

  private String operator;

  private String filterValue;

  private String entityProperty; //TODO: to be modified

  @Valid
  private Set<TargetRequest> targets;
}
