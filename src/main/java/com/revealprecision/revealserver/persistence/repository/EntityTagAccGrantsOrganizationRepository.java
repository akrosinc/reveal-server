package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.EntityTagAccGrantsOrganization;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EntityTagAccGrantsOrganizationRepository extends JpaRepository<EntityTagAccGrantsOrganization, UUID> {

  List<EntityTagAccGrantsOrganization> findAllByEntityTag_IdentifierAndOrganizationIdIn(UUID entityTagId, List<UUID> orgIds);

  @Query("SELECT DISTINCT etago.entityTag.tag FROM EntityTagAccGrantsOrganization etago " +
      "JOIN etago.entityTag et " +
      "WHERE etago.organizationId IN (" +
      "SELECT o.identifier FROM User u " +
      "JOIN u.organizations o " +
      "WHERE u.identifier = :userId " +
      "AND o.instance.identifier = :instanceId)")
  List<String> findDatasetsByUserIdAndInstanceId(UUID userId, UUID instanceId);

  List<EntityTagAccGrantsOrganization> findByOrganizationId(UUID identifier);

  void deleteByOrganizationId(UUID organizationId);
}
