package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.OrganizationRoleMapping;
import com.revealprecision.revealserver.persistence.domain.id.OrganizationRoleMappingId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OrganizationRoleMappingRepository extends
    JpaRepository<OrganizationRoleMapping, OrganizationRoleMappingId> {

  @Modifying
  @Query("DELETE FROM OrganizationRoleMapping rm WHERE rm.organization.identifier = :organizationId")
  void deleteByOrganizationIdentifier(UUID organizationId);

  @Query("SELECT DISTINCT rm.organizationRoleId FROM OrganizationRoleMapping rm " +
      "WHERE rm.organization.identifier = :organizationId " +
      "AND rm.organization.identifier IN (" +
      "SELECT o.identifier FROM User u " +
      "JOIN u.organizations o " +
      "WHERE u.identifier = :userId)")
  List<UUID> findRoleIdsByUserAndOrganization(UUID userId, UUID organizationId);

  List<OrganizationRoleMapping> findByOrganization_Identifier(UUID organizationId);
}
