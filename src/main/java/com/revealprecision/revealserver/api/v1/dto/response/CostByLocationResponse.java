package com.revealprecision.revealserver.api.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.revealprecision.revealserver.model.CostContainer;
import com.revealprecision.revealserver.persistence.domain.CostByLocation;
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
public class CostByLocationResponse {

  private UUID identifier;
  private UUID locationIdentifier;
  private UUID actionIdentifier;
  private CostContainer costContainer;
  private Double calculatedValue;

  public static CostByLocationResponse fromEntity(CostByLocation entity) {
    return CostByLocationResponse.builder()
        .identifier(entity.getIdentifier())
        .locationIdentifier(
            entity.getLocation() != null ? entity.getLocation().getIdentifier() : null)
        .actionIdentifier(
            entity.getAction() != null ? entity.getAction().getIdentifier() : null)
        .costContainer(entity.getCostContainer())
        .calculatedValue(entity.getCalculatedValue())
        .build();
  }
}
