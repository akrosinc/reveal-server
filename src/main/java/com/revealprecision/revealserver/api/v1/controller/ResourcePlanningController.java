package com.revealprecision.revealserver.api.v1.controller;

import com.revealprecision.revealserver.api.v1.dto.factory.CountryCampaignFactory;
import com.revealprecision.revealserver.api.v1.dto.response.CountryCampaignResponse;
import com.revealprecision.revealserver.persistence.repository.CountryCampaignRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/resource-planning")
public class ResourcePlanningController {

  private final CountryCampaignRepository countryCampaignRepository;

  @GetMapping
  public ResponseEntity<List<CountryCampaignResponse>> getCountries() {
    return ResponseEntity.ok()
        .body(countryCampaignRepository.findAll().stream().map(CountryCampaignFactory::fromEntity).collect(
            Collectors.toList()));
  }
}
