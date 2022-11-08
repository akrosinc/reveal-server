package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.EventTracker;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventTrackerRepository extends JpaRepository<EventTracker, UUID>{

  Optional<EventTracker> findEventTrackerByAggregationKey(String aggregationKey);

}
