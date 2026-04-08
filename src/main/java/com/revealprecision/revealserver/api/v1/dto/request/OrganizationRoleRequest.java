package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.List;
import java.util.UUID;
import javax.validation.constraints.NotBlank;
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
public class OrganizationRoleRequest {
  @NotBlank(message = "name must not be empty")
  private String name;
  private List<UUID> permissionIdentifiers;
}
