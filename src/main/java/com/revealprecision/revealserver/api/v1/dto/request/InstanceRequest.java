package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InstanceRequest {
  @Valid
  private PlanRequest planRequest;

  @NotNull
  private String instanceName;

  @NotNull
  private UUID locationHierarchy;

  @NotNull
  @Min(1)
  private List<UUID> areas;

  @NotNull
  @Min(1)
  private List<UUID> members;

  private List<UUID> datasets_tags;

  private Set<Integer> complexTags;
}
