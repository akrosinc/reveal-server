package com.revealprecision.revealserver.api.v1.facade.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.Data;

@Data
public class HdssSyncRequest implements Serializable  {

  @JsonProperty(value = "user_id")
  private String userId;

  private long serverVersion;

  private int batchSize;
}
