package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.enums.ProcessTrackerEnum;
import com.revealprecision.revealserver.enums.ProcessType;
import com.revealprecision.revealserver.persistence.domain.ProcessTracker;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessTrackerRepository extends EntityGraphJpaRepository<ProcessTracker, UUID> {

  List<ProcessTracker> findProcessTrackerByPlanIdentifierAndProcessTypeAndState(UUID planIdentifier, ProcessType processType, ProcessTrackerEnum trackerEnum);

  List<ProcessTracker> findProcessTrackerByPlanIdentifierAndProcessType(UUID planIdentifier,
      ProcessType processType);


}