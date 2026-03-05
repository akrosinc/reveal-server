package com.revealprecision.revealserver.persistence.domain.id;

import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class OrganizationRolePermissionId implements Serializable {

    @Column(name = "organization_role_id")
    private UUID organizationRoleId;

    @Column(name = "permission_id")
    private UUID permissionId;
}