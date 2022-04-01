package com.revealprecision.revealserver.api.v1.facade.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Data
@Setter
@Getter
@Builder
@FieldNameConstants
@JsonInclude(value = Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class EventClientFacadeResponse implements Serializable {

  @JsonProperty("failed_events")
  private List<UUID> failedEvents;

  @JsonProperty("failed_clients")
  private List<UUID> failedClients;
}

