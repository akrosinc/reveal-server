package com.revealprecision.revealserver.api.v1.facade.dto.response;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Team {

  public String teamName;

  public String display;

  public TeamLocation location;

  public String teamIdentifier;

  public String uuid;

  public List<UUID> organizationIds;
}