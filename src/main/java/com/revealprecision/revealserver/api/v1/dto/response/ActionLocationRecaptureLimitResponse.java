package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.revealprecision.revealserver.persistence.domain.ActionLocationRecaptureLimit;
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
public class ActionLocationRecaptureLimitResponse {

  private UUID identifier;
  private UUID instanceIdentifier;
  private UUID locationIdentifier;
  private UUID actionIdentifier;
  private Integer count;

  public static ActionLocationRecaptureLimitResponse fromEntity(ActionLocationRecaptureLimit entity) {
    return ActionLocationRecaptureLimitResponse.builder()
        .identifier(entity.getIdentifier())
        .instanceIdentifier(
            entity.getInstance() != null ? entity.getInstance().getIdentifier() : null)
        .locationIdentifier(
            entity.getLocation() != null ? entity.getLocation().getIdentifier() : null)
        .actionIdentifier(
            entity.getAction() != null ? entity.getAction().getIdentifier() : null)
        .count(entity.getCount())
        .build();
  }
}
