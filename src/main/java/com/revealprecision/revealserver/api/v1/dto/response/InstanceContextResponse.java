package com.revealprecision.revealserver.api.v1.dto.response;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstanceContextResponse {
  private IdentifierNameResponse selectedInstance;
  private IdentifierNameResponse instancePlan;
  private InstanceRoleInfo role;
  private List<GroupContextInfo> groups;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class InstanceRoleInfo {
    private UUID identifier;
    private String name;
    private Set<String> permissions;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GroupContextInfo {
    private UUID identifier;
    private String name;
    private String type;
    private List<GroupRoleInfo> roles;
  }


  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GroupRoleInfo {
    private UUID identifier;
    private String name;
    private Set<String> permissions;
  }
}


