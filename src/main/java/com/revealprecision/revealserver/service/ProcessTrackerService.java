package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.ProcessTrackerEnum;
import com.revealprecision.revealserver.enums.ProcessType;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.Plan;
import com.revealprecision.revealserver.persistence.domain.ProcessTracker;
import com.revealprecision.revealserver.persistence.repository.ProcessTrackerRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ProcessTrackerService {

  private final ProcessTrackerRepository processTrackerRepository;

  public Optional<ProcessTracker> findByIdentifier(UUID identifier) {
    return processTrackerRepository.findById(identifier);
  }

  public ProcessTracker createProcessTracker(UUID processId, ProcessType processType,
      UUID planIdentifier) {
    ProcessTracker processTracker = ProcessTracker.builder()
        .processTriggerIdentifier(processId)
        .planIdentifier(planIdentifier)
        .state(ProcessTrackerEnum.NEW)
        .processType(processType)
        .build();
    processTracker.setEntityStatus(EntityStatus.ACTIVE);
    return processTrackerRepository.save(
        processTracker);
  }

  public ProcessTracker updateProcessTracker(UUID identifier, ProcessTrackerEnum state) {
    ProcessTracker processTracker = processTrackerRepository.findById(identifier).orElseThrow(
        () -> new NotFoundException(Pair.of(ProcessTracker.Fields.identifier, identifier),
            ProcessTracker.class));
    processTracker.setState(state);

    return processTrackerRepository.save(processTracker);
  }

  public List<ProcessTracker> findProcessTrackerByPlanAndProcessType(UUID planIdentifier,
      ProcessType processType) {
    return processTrackerRepository.findProcessTrackerByPlanIdentifierAndProcessType(planIdentifier,
        processType);
  }

  public List<ProcessTracker> findProcessTrackerByPlanIdentifierAndProcessTypeAndState(Plan plan,
      ProcessType processType, ProcessTrackerEnum trackerEnum) {
    return processTrackerRepository.findProcessTrackerByPlanIdentifierAndProcessTypeAndState(
        plan.getIdentifier(), processType, trackerEnum);
  }

}
