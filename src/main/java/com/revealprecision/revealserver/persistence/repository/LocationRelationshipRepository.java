package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.LocationRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LocationRelationshipRepository extends JpaRepository<LocationRelationship, UUID> {
}
