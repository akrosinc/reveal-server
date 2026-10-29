package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.persistence.domain.ActionLocationRecaptureLimit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionLocationRecaptureLimitRepository extends
    EntityGraphJpaRepository<ActionLocationRecaptureLimit, UUID> {

  List<ActionLocationRecaptureLimit> findByInstance_Identifier(UUID instanceIdentifier);

  List<ActionLocationRecaptureLimit> findByLocation_Identifier(UUID locationIdentifier);

  List<ActionLocationRecaptureLimit> findByAction_Identifier(UUID actionIdentifier);

  Optional<ActionLocationRecaptureLimit> findByInstance_IdentifierAndLocation_IdentifierAndAction_Identifier(
      UUID instanceIdentifier, UUID locationIdentifier, UUID actionIdentifier);
}
