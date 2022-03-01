package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.persistence.domain.PlanLocations;
import com.revealprecision.revealserver.persistence.repository.PlanLocationsRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class PlanLocationsService {

  private final PlanLocationsRepository planLocationsRepository;

  @Autowired
  public PlanLocationsService(PlanLocationsRepository planLocationsRepository){
    this.planLocationsRepository = planLocationsRepository;
  }


  public List<PlanLocations> getPlanLocationsByPlanIdentifier(UUID planIdentifier) {
    return planLocationsRepository.findByPlan_Identifier(planIdentifier);
  }

  public List<PlanLocations> getPlanLocationsByLocationIdentifier(UUID locationIdentifier) {
    return planLocationsRepository.findByLocation_Identifier(locationIdentifier);
  }

  public List<PlanLocations> getPlanLocationsByLocationIdentifierList(List<UUID> locationIdentifiers) {
    return planLocationsRepository.findByLocation_IdentifierIn(locationIdentifiers);
  }

}