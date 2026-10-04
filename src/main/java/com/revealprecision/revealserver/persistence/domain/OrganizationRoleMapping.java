package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.persistence.domain.id.OrganizationRoleMappingId;
import java.util.UUID;
import javax.persistence.Column;
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
@Table(name = "organization_role_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class OrganizationRoleMapping {
  @EmbeddedId
  private OrganizationRoleMappingId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("organizationId")
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @Column(name = "organization_role_id", insertable = false, updatable = false)
  private UUID organizationRoleId;

  public void populate(final Organization organization, final UUID organizationRoleId) {
    this.organization = organization;
    this.organizationRoleId = organizationRoleId;
    this.id = new OrganizationRoleMappingId(organizationRoleId, organization.getIdentifier());
  }
}
