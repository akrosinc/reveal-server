package com.revealprecision.revealserver.api.v1.facade.models;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LoginResponse {

  private UserFacadeResponse user;
  private LocationTree locations;
  private TeamMember team;
  private List<String> jurisdictions;
  private Set<String> jurisdictionIds;
}
