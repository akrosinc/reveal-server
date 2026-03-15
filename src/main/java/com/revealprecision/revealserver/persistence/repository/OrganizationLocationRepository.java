package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.OrganizationLocation;
import com.revealprecision.revealserver.persistence.domain.id.OrganizationLocationId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrganizationLocationRepository extends
    JpaRepository<OrganizationLocation, OrganizationLocationId> {

  @Query("SELECT DISTINCT ol.location.identifier FROM OrganizationLocation ol " +
      "JOIN ol.organization o " +
      "JOIN o.users u " +
      "WHERE o.instance.identifier = :instanceIdentifier " +
      "AND u.identifier = :userId")
  List<UUID> findLocationIdentifiersByInstanceAndUser(UUID instanceIdentifier, UUID userId);
}
