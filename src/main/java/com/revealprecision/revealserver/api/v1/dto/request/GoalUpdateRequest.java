package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.PriorityEnum;
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
public class GoalUpdateRequest {

  @NotBlank
  private String description;

  @NotNull
  private PriorityEnum priority;

}
