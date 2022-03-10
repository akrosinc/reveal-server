package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.EntityTags;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntityTagRepository extends JpaRepository<EntityTags, UUID> {

  Set<EntityTags> findByLookupEntityType_Identifier(UUID lookupEntityTypeIdentifier);
}
