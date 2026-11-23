package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.InstanceCustomLocation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstanceCustomLocationRepository extends
    JpaRepository<InstanceCustomLocation, UUID> {

  Optional<InstanceCustomLocation> findByIdentifierAndInstance_Identifier(UUID identifier,
      UUID instanceIdentifier);

  // sites of an instance under a shared parent (Health Facility)
  List<InstanceCustomLocation> findAllByInstance_IdentifierAndParentLocation_IdentifierOrderByName(
      UUID instanceIdentifier, UUID parentLocationIdentifier);

  // sites of an instance under any of the given shared parents (Health Facilities)
  List<InstanceCustomLocation> findAllByInstance_IdentifierAndParentLocation_IdentifierInOrderByName(
      UUID instanceIdentifier, Collection<UUID> parentLocationIdentifiers);

  // points of an instance belonging to the given sites
  List<InstanceCustomLocation> findAllByInstance_IdentifierAndParent_IdentifierIn(
      UUID instanceIdentifier, Collection<UUID> parentIdentifiers);
}
