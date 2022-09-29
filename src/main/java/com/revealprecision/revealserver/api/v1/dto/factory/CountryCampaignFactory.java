package com.revealprecision.revealserver.api.v1.dto.factory;

import com.revealprecision.revealserver.api.v1.dto.response.AgeGroupResponse;
import com.revealprecision.revealserver.api.v1.dto.response.CountryCampaignResponse;
import com.revealprecision.revealserver.props.CountryCampaign;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CountryCampaignFactory {

  public static CountryCampaignResponse fromEntity(CountryCampaign countryCampaign) {
    List<AgeGroupResponse> ageGroupResponses = countryCampaign.getGroups().stream().map(el -> AgeGroupResponse.builder()
        .max(el.getMax())
        .min(el.getMin())
        .name(el.getName())
        .key(el.getKey())
        .build()).collect(Collectors.toList());
    return CountryCampaignResponse.builder()
        .name(countryCampaign.getName())
        .identifier(countryCampaign.getIdentifier())
        .ageGroups(ageGroupResponses)
        .build();
  }
}
