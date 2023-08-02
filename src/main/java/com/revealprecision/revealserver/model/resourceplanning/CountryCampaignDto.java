package com.revealprecision.revealserver.model.resourceplanning;

import com.revealprecision.revealserver.persistence.domain.AgeGroup;
import java.util.List;
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
public class CountryCampaignDto {

  private UUID identifier;


  private String name;

  private List<AgeGroup> ageGroups;

  private String key;
}
