package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.persistence.domain.id.OrganizationRolePermissionId;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "organization_role_permission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class OrganizationRolePermission {
  @EmbeddedId
  private OrganizationRolePermissionId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("organizationRoleId")
  @JoinColumn(name = "organization_role_id")
  private OrganizationRole organizationRole;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("permissionId")
  @JoinColumn(name = "permission_id")
  private Permission permission;
}
