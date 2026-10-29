package com.revealprecision.revealserver.persistence.repository;

import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import com.revealprecision.revealserver.persistence.domain.CostByLocation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface CostByLocationRepository extends EntityGraphJpaRepository<CostByLocation, UUID> {

  List<CostByLocation> findByLocation_Identifier(UUID locationIdentifier);

  List<CostByLocation> findByAction_Identifier(UUID actionIdentifier);

  Optional<CostByLocation> findByLocation_IdentifierAndAction_Identifier(UUID locationIdentifier,
      UUID actionIdentifier);
}
