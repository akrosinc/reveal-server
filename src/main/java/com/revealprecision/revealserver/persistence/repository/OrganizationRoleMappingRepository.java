package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.OrganizationRole;
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

  @Query("SELECT DISTINCT rm FROM OrganizationRoleMapping rm " +
      "JOIN FETCH rm.organizationRole r " +
      "LEFT JOIN FETCH r.permissions p " +
      "LEFT JOIN FETCH p.permission " +
      "WHERE rm.organization.identifier = :organizationId " +
      "AND rm.organization.identifier IN (" +
      "SELECT o.identifier FROM User u " +
      "JOIN u.organizations o " +
      "WHERE u.identifier = :userId)")
  List<OrganizationRoleMapping> findRolesByUserAndOrganization(UUID userId, UUID organizationId);

}
