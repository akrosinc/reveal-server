package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.OrganizationRolePermission;
import com.revealprecision.revealserver.persistence.domain.id.OrganizationRolePermissionId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OrganizationRolePermissionRepository extends
    JpaRepository<OrganizationRolePermission, OrganizationRolePermissionId> {
  @Modifying
  @Query("DELETE FROM OrganizationRolePermission p WHERE p.organizationRole.identifier = :roleId")
  void deleteByOrganizationRoleIdentifier(UUID roleId);
}
