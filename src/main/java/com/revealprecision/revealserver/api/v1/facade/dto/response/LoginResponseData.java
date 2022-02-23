package com.revealprecision.revealserver.api.v1.facade.dto.response;

import com.revealprecision.revealserver.api.v1.dto.response.GeoTree;
import com.revealprecision.revealserver.persistence.domain.User;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LoginResponseData {
  public UserFacadeResponse user;
  public LocationTree locations;
  public TeamMember team;
  public List<String> jurisdictions;
  public Set<String> jurisdictionIds;
}
