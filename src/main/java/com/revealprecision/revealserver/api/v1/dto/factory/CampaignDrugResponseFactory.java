package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.CampaignDrugResponse;
import com.revealprecision.revealserver.api.v1.dto.response.DrugResponse;
import com.revealprecision.revealserver.persistence.domain.CampaignDrug;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CampaignDrugResponseFactory {

  public static CampaignDrugResponse fromEntity(CampaignDrug campaignDrug) {
    List<DrugResponse> drugResponses = campaignDrug.getDrugs().stream().map(el -> DrugResponse.builder()
        .max(el.getMax())
        .min(el.getMin())
        .name(el.getName())
        .full(el.isFull())
        .half(el.isHalf())
        .millis(el.isMillis())
        .key(el.getKey())
        .build()).collect(Collectors.toList());
    return CampaignDrugResponse.builder()
        .identifier(campaignDrug.getIdentifier())
        .name(campaignDrug.getName())
        .drugs(drugResponses)
        .build();
  }
}
