package com.revealprecision.revealserver.api.v1.facade.models;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalFacade {

  private String id;

  private String description;

  private String priority;

  private List<TargetFacade> targets;
}