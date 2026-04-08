package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.OrganizationLocation;
import com.revealprecision.revealserver.persistence.domain.id.OrganizationLocationId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OrganizationLocationRepository extends
    JpaRepository<OrganizationLocation, OrganizationLocationId> {

  @Query("SELECT DISTINCT ol.location.identifier FROM OrganizationLocation ol " +
      "JOIN ol.organization o " +
      "JOIN o.users u " +
      "WHERE o.instance.identifier = :instanceIdentifier " +
      "AND u.identifier = :userId")
  List<UUID> findLocationIdentifiersByInstanceAndUser(UUID instanceIdentifier, UUID userId);

  @Query("SELECT ol FROM OrganizationLocation ol " +
      "JOIN FETCH ol.location " +
      "WHERE ol.organization.identifier = :organizationId")
  List<OrganizationLocation> findByOrganizationIdentifier(UUID organizationId);

  @Modifying
  @Query("DELETE FROM OrganizationLocation ol WHERE ol.organization.identifier = :organizationId")
  void deleteByOrganizationIdentifier(UUID organizationId);

  @Query(value = "SELECT DISTINCT ol.location_id " +
      "FROM organization_location ol " +
      "JOIN user_organization uo ON uo.organization_identifier = ol.organization_id " +
      "JOIN organization o ON o.identifier = ol.organization_id " +
      "WHERE uo.user_identifier = :userId " +
      "AND o.instance_id IN :instanceIds " +
      "AND o.entity_status = 'ACTIVE'",
      nativeQuery = true)
  List<UUID> findAllLocationIdentifiersByUserIdAndInstanceIds(UUID userId, List<UUID> instanceIds);
}
