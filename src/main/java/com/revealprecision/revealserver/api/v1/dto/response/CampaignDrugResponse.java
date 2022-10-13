package com.revealprecision.revealserver.api.v1.dto.response;

import java.util.List;
import java.util.UUID;
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
public class CampaignDrugResponse {
  private UUID identifier;
  private String name;
  private List<DrugResponse> drugs;
}
