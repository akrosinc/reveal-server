package com.revealprecision.revealserver.api.v1.facade.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TeamMember {

  private String identifier;

  private String teamMemberId;

  private Team team;

  private String uuid;
}
