package com.revealprecision.revealserver.persistence.repository;

import com.revealprecision.revealserver.persistence.domain.OrganizationRole;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrganizationRoleRepository extends
    JpaRepository<OrganizationRole, UUID> {

  @Query("SELECT r FROM OrganizationRoleMapping rm " +
      "JOIN rm.organizationRole r " +
      "WHERE rm.organization.identifier = :organizationId")
  List<OrganizationRole> findByOrganizationId(UUID organizationId);
}
