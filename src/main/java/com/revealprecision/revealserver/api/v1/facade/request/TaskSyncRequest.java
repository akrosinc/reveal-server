package com.revealprecision.revealserver.api.v1.facade.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskSyncRequest {

  @JsonProperty
  private List<String> plan = new ArrayList<>();

  @JsonProperty
  private List<UUID> group = new ArrayList<>();

  @JsonProperty
  private long serverVersion;

  @JsonProperty
  private String owner;

  @JsonProperty("return_count")
  private boolean returnCount;

}
