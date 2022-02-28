package com.revealprecision.revealserver.api.v1.facade.dto.response;

import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAssignmentResponse {

  private Set<UUID> organizationIds;

  private Set<String> jurisdictions;

  private Set<String> plans;

}