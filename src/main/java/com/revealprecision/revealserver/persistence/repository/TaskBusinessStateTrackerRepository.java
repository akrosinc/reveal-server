package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.TaskBusinessStateTracker;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskBusinessStateTrackerRepository extends
    JpaRepository<TaskBusinessStateTracker, UUID> {

  List<TaskBusinessStateTracker> findFirstByLocationHierarchyIdentifierAndTaskLocationIdentifierAndPlanIdentifier(
      UUID locationHierarchyIdentifier, UUID taskLocationIdentifier, UUID planIdentifier);
}
