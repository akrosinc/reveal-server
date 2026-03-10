package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.persistence.domain.id.InstanceRolePermissionId;
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
@Table(name = "instance_role_permission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class InstanceRolePermission {
  @EmbeddedId
  private InstanceRolePermissionId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("instanceRoleId")
  @JoinColumn(name = "instance_role_id")
  private InstanceRole instanceRole;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("permissionId")
  @JoinColumn(name = "permission_id")
  private Permission permission;

  public void populate(final InstanceRole instanceRole, final Permission permission) {
    this.instanceRole = instanceRole;
    this.permission = permission;
    this.id = new InstanceRolePermissionId(instanceRole.getIdentifier(), permission.getIdentifier());
  }
}
