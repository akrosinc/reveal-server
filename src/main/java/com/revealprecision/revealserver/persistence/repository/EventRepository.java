package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.Event;
import com.revealprecision.revealserver.persistence.projection.EventMaxVersionProjection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID>,
    JpaSpecificationExecutor<Event> {

  @Query(value = "select max(version) as version, CAST(identifier as varchar) as identifier, event_type as eventType from event where task_identifier = :taskIdentifier and event_type= :eventType group by identifier, event_type ", nativeQuery = true)
  List<EventMaxVersionProjection> findEventByMaxVersionAndTaskIdentifier(UUID taskIdentifier,
      String eventType);
}
