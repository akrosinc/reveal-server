package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConditionRequest {

  private String name;

  private String query;

//  @Valid
  private Set<TargetRequest> targets;
}
