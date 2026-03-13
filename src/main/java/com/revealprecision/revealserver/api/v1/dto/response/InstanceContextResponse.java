package com.revealprecision.revealserver.api.v1.dto.response;

import java.util.List;
import java.util.Set;
import java.util.UUID;
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
public class InstanceContextResponse {
  private IdentifierNameResponse selectedInstance;
  private UUID roleIdentifier;
  private String roleName;
  private Set<String> permissions;
}
