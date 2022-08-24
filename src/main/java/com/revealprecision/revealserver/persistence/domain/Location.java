package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.api.v1.dto.request.LocationRequest;
import com.revealprecision.revealserver.enums.LocationStatus;
import com.revealprecision.revealserver.persistence.generator.LocationServerVersionGenerator;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.GeneratorType;
import org.hibernate.annotations.GenericGenerator;
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
public class Location extends AbstractAuditableEntity {

  @Id
  @GeneratedValue(generator = "custom-generator")
  @GenericGenerator(name = "custom-generator", strategy = "com.revealprecision.revealserver.persistence.generator.CustomIdentifierGenerator")
  private UUID identifier;

  @ColumnDefault(value = "feature")
  private String type;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private Geometry geometry;

  private String name;

  @Enumerated(EnumType.STRING)
  private LocationStatus status;
  private UUID externalId;

  private String hashValue;

  @GeneratorType(type = LocationServerVersionGenerator.class, when = GenerationTime.ALWAYS)
  private Long serverVersion;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "geographic_level_identifier")
  private GeographicLevel geographicLevel;

  @ManyToOne
  @JoinColumn(name = "location_bulk_identifier")
  private LocationBulk locationBulk;

  @ManyToMany(mappedBy = "locations")
  private Set<Person> people;

  public Location(UUID identifier, String type, String name, LocationStatus status, UUID externalId,
      GeographicLevel geographicLevel,
      LocationBulk locationBulk) {
    this.identifier = identifier;
    this.type = type;
    this.name = name;
    this.status = status;
    this.externalId = externalId;
    this.geographicLevel = geographicLevel;
    this.locationBulk = locationBulk;
  }

  public Location(UUID identifier, UUID externalId, String geographicLevelName, String name) {
    this.identifier = identifier;
    this.name = name;
    this.externalId = externalId;
    this.geographicLevel = GeographicLevel.builder().name(geographicLevelName).build();
  }

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
