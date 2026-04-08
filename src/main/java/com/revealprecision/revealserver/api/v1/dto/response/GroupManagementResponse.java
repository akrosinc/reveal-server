package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class GroupManagementResponse {
  private UUID identifier;
  private String name;
  private String type;
  private boolean active;
  private List<IdentifierNameResponse> members;
  private List<IdentifierNameResponse> datasets;
  private List<IdentifierNameResponse> roles;
  private List<GeoTreeResponse> areas;
}
