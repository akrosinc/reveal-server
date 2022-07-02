package com.revealprecision.revealserver.api.v1.dto.request;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DataFilterRequest {

  private UUID hierarchyIdentifier;
  private UUID locationIdentifier;
  private List<EntityFilterRequest> entityFilters;
}
