package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.persistence.domain.CampaignDrug;
import com.revealprecision.revealserver.persistence.domain.CountryCampaign;
import com.revealprecision.revealserver.persistence.repository.CampaignDrugRepository;
import com.revealprecision.revealserver.persistence.repository.CountryCampaignRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourcePlanningService {

  private final CountryCampaignRepository countryCampaignRepository;
  private final CampaignDrugRepository campaignDrugRepository;

  public List<CountryCampaign> getCountries() {
    return countryCampaignRepository.findAll();
  }

  public List<CampaignDrug> getCampaigns() {
    return campaignDrugRepository.findAll();
  }
}
