package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.enums.LocationStatus;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.locationtech.jts.geom.Geometry;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

@FieldNameConstants
@Entity
@Table(name = "instance_custom_location")
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@Getter
@Setter
@SQLDelete(sql = "UPDATE instance_custom_location SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class InstanceCustomLocation extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "instance_id", nullable = false, updatable = false)
  private Instance instance;

  private String name;

  @Enumerated(EnumType.STRING)
  private LocationStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "geographic_level_identifier")
  private GeographicLevel geographicLevel;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_location_identifier")
  private Location parentLocation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_identifier")
  private InstanceCustomLocation parent;

  // PostGIS geometry(Geometry,4326); not audited (the _aud table has no geometry column)
  @NotAudited
  @Column(name = "geometry", columnDefinition = "geometry(Geometry,4326)")
  private Geometry geometry;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private LocationProperty metadata;

  public boolean isCustomLocation() {
    return parentLocation != null;
  }
}
