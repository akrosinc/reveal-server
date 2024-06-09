package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.EntityTagOwnership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntityTagOwnershipRepository extends JpaRepository<EntityTagOwnership, UUID> {


  Optional<List<EntityTagOwnership>> getEntityTagOwnershipsByEntityTag_IdentifierInAndUserSid(List<UUID> entityTagId,UUID userSid);
}
