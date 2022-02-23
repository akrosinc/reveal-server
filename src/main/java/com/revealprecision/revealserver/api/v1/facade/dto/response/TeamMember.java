package com.revealprecision.revealserver.api.v1.facade.dto.response;

import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TeamMember {

  public String identifier;

  public String teamMemberId;

  public Set<TeamLocation> locations;

  public Team team;

  public String uuid;
}
