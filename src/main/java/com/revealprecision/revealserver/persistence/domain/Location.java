package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

@FieldNameConstants
@Entity
@Audited
@Getter
@Setter
@SQLDelete(sql = "UPDATE location SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@NamedNativeQueries({
    @NamedNativeQuery(name = "Location.hasParentChildRelationship", query = "select ST_Contains (ST_AsText(ST_GeomFromGeoJSON(?)),ST_AsText(ST_Centroid(ST_GeomFromGeoJSON(?))))")
})
public class Location extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  @ColumnDefault(value = "feature")
  private String type;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private Geometry geometry;

  private String name;
  private String status;
  private UUID externalId;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "geographic_level_identifier")
  private GeographicLevel geographicLevel;

  public Location update(LocationRequest locationRequest, GeographicLevel geographicLevel) {
    this.type = locationRequest.getType();
    this.name = locationRequest.getProperties().getName();
    this.status = locationRequest.getProperties().getStatus();
    this.externalId = locationRequest.getProperties().getExternalId();
    this.geometry = locationRequest.getGeometry();
    this.geographicLevel = geographicLevel;
    return this;
  }
}
