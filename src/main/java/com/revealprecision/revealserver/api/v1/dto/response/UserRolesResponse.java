package com.revealprecision.revealserver.api.v1.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRolesResponse {
  private List<InstanceInfo> instanceInfos;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class InstanceInfo {
    private IdentifierNameResponse instanceRole;
    private List<GroupRoleInfo> groupRoles;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GroupRoleInfo {
    private GroupIdentifierNameTypeResponse group;
    private List<IdentifierNameResponse> roles;
  }
}
