package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.PerformanceEventTracker;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PerformanceEventTrackerRepository extends JpaRepository<PerformanceEventTracker, UUID> {
  PerformanceEventTracker findPerformanceEventTrackerBySubmissionId(String submissionId);
}
