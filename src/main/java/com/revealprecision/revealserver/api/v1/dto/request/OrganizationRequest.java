package com.revealprecision.revealserver.api.v1.dto.request;

import com.revealprecision.revealserver.enums.OrganizationTypeEnum;
import java.util.UUID;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationRequest {

  @NotBlank(message = "must not be empty")
  private String name;

  @NotNull(message = "must not be null")
  private OrganizationTypeEnum type;

  @NotNull(message = "must not be null")
  private boolean active;

  @Parameter(description = "Parent organization identifier")
  private UUID partOf;
}
