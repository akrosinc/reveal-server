package com.revealprecision.revealserver.api.v1.dto.response;

import com.revealprecision.revealserver.enums.PriorityEnum;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalResponse {

  private UUID identifier;
  private PriorityEnum priority;
  private String description;
  private Set<TargetResponse> targets;
  private Set<ActionResponse> actions;
}
