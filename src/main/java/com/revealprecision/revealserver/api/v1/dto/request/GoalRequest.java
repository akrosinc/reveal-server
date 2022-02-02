package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.PriorityEnum;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GoalRequest {

  @NotBlank
  private String identifier;

  @NotBlank
  private String description;

  @NotNull
  private PriorityEnum priority;

  @Valid
  private Set<TargetRequest> targets;

  @Valid
  private Set<ActionRequest> actions;
}
