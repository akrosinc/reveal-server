package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.persistence.domain.id.InstanceLocationId;
import com.revealprecision.revealserver.persistence.domain.id.OrganizationLocationId;
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
@Table(name = "organization_location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class OrganizationLocation {

  @EmbeddedId
  private OrganizationLocationId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("organizationId")
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("locationId")
  @JoinColumn(name = "location_id")
  private Location location;
}

