package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Event;
import com.revealprecision.revealserver.persistence.domain.EventTracker;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface EventTrackerRepository extends JpaRepository<EventTracker, UUID>{

  Optional<EventTracker> findFirstEventTrackerByEventIdentifierAndEntityTagIdentifier(UUID eventIdentifier, UUID entityTagIdentifier);

  Optional<EventTracker> findFirstEventTrackerByEventIdentifierAndEntityTagIdentifierAndDate(UUID eventIdentifier, UUID entityTagIdentifier, String localDateTime);

}
