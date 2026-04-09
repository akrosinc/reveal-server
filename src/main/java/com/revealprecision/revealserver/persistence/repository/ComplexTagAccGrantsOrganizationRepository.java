package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.ComplexTag;
import com.revealprecision.revealserver.persistence.domain.ComplexTagAccGrantsOrganization;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplexTagAccGrantsOrganizationRepository extends JpaRepository<ComplexTagAccGrantsOrganization, UUID> {

  List<ComplexTagAccGrantsOrganization> findAllByComplexTag_IdAndOrganizationIdIn(Integer id, List<UUID> orgIds);

  List<ComplexTagAccGrantsOrganization> findByOrganizationId(UUID identifier);

  @Query("SELECT DISTINCT ctago.complexTag " +
      "FROM ComplexTagAccGrantsOrganization ctago " +
      "WHERE ctago.organizationId IN (" +
      "SELECT o.identifier FROM User u " +
      "JOIN u.organizations o " +
      "WHERE u.identifier = :userId " +
      "AND o.instance.identifier = :instanceId)")
  Set<ComplexTag> findByUserIdAndInstanceId(UUID userId, UUID instanceId);
}
