package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocationPropertyResponse {

  private String name;
  private String status;
  private UUID externalId;
  private String geographicLevel;
  private Long numberOfTeams;
  private boolean assigned;
  private UUID parentIdentifier;
}
