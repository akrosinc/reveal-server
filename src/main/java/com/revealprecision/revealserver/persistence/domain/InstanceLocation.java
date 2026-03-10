package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.persistence.domain.id.InstanceLocationId;
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
@Table(name = "instance_location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class InstanceLocation {

  @EmbeddedId
  private InstanceLocationId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("instanceId")
  @JoinColumn(name = "instance_id")
  private Instance instance;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("locationId")
  @JoinColumn(name = "location_id")
  private Location location;

  public void populate(final Instance instance, final Location location) {
    this.instance = instance;
    this.location = location;
    this.id = new InstanceLocationId(instance.getIdentifier(), location.getIdentifier());
  }
}

