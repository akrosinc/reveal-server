package com.revealprecision.revealserver.api.v1.facade.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class LocationSyncRequest {
  @JsonProperty("is_jurisdiction")
  private Boolean isJurisdiction;

  @JsonProperty("location_names")
  private List<String> locationNames;

  @JsonProperty("location_ids")
  private List<String> locationIds;

  @JsonProperty("parent_id")
  private List<String> parentId;

  @JsonProperty
  private long serverVersion;

  @JsonProperty("return_count")
  private boolean returnCount;

  @JsonProperty
  private String hierarchyIdentifier;
}
