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
  public UserFacadeResponse user;
  public LocationTree locations;
  public TeamMember team;
  public List<String> jurisdictions;
  public Set<String> jurisdictionIds;
}
