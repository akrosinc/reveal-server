package com.revealprecision.revealserver.api.v1.facade.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SyncParamFacade {

  private String providerId;
  private String locationId;
  private String baseEntityId;
  private Long serverVersion;
  private String team;
  private String teamId;
  private Integer limit;
  @JsonProperty("return_count")
  private boolean returnCount;
}
