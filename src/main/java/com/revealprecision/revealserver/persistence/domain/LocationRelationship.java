package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.persistence.projection.LocationRelationshipProjection;
import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "UPDATE location_relationship SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
public class LocationRelationship extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  @ManyToOne
  @JoinColumn(name = "location_hierarchy_identifier")
  private LocationHierarchy locationHierarchy;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "location_identifier")
  private Location location;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_identifier")
  private Location parentLocation;

  @Type(type = "list-array")
  private List<UUID> ancestry;

  public LocationRelationship(LocationRelationshipProjection projection) {
    this.identifier = UUID.fromString(projection.getIdentifier());
    Location location = Location.builder().
        identifier(UUID.fromString(projection.getLocationIdentifier()))
        .name(projection.getLocationName())
        .geographicLevel(GeographicLevel.builder().name(projection.getGeographicLevelName()).build())
        .build();
    this.location = location;
    if(projection.getParentIdentifier() != null) {
      Location parentLocation = Location.builder()
          .identifier(UUID.fromString(projection.getParentIdentifier()))
          .geographicLevel(GeographicLevel.builder().name(projection.getGeographicLevelName()).build())
          .build();
      this.parentLocation = parentLocation;
    }
  }
}
