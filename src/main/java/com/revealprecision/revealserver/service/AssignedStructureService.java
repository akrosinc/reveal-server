package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.persistence.repository.PlanLocationsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AssignedStructureService {

  private final PlanLocationsRepository planLocationsRepository;

  @Async
  public void refreshAssignedStructureCountsMaterializedView(){
    planLocationsRepository.refreshAssignedStructureCountsMaterializedView();
  }
}
