package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.EntityData;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntityDataRepository extends JpaRepository<EntityData, UUID> {

  Optional<EntityData> findByIdentifier(UUID identifier);

  List<EntityData> findByIdentifierIn(Collection<UUID> identifiers);
}
