package com.revealprecision.revealserver.api.v1.facade.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TeamLocation {

  public String uuid;

  public String name;

  public String display;
}
