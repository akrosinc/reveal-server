package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroupManagementRequest {
  private String name;
  private Boolean isTeam;

  @NotNull
  private List<UUID> areasIdentifiers;

  @NotNull
  private List<UUID> rolesIdentifiers;

  @NotNull
  private List<UUID> datasetsIdentifiers;

  @NotNull
  private Set<Integer> complexTagIdentifiers;

  @NotNull
  private List<UUID> membersIdentifiers;
}
