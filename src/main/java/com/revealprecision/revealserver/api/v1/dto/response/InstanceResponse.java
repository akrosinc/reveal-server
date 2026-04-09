package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceResponse {
  private UUID identifier;
  private String name;
  private PlanResponse plan;

  private List<IdentifierNameResponse> members;

  private List<GeoTreeResponse> areas;

  private List<LocationHierarchyResponse> locationHierarchy;

  private List<IdentifierNameResponse> datasets;

  private List<IdNameResponse> complexTags;
}
