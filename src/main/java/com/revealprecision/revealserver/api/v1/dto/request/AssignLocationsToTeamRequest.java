package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.Set;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AssignLocationsToTeamRequest {

  @NotNull
  private UUID organizationIdentifier;

  @NotNull
  private Set<UUID> locationIdentifiers;
}
