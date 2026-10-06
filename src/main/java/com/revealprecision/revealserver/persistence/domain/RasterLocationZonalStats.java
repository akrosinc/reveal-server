package com.revealprecision.revealserver.persistence.domain;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

@FieldNameConstants
@Audited
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "raster_location_zonal_stats")
@SQLDelete(sql = "UPDATE raster_location_zonal_stats SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
public class RasterLocationZonalStats extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  @Column(name = "raster_id", nullable = false)
  private String rasterId;

  @Column(name = "tag")
  private String tag;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "location_identifier", referencedColumnName = "identifier", nullable = false)
  private Location location;

  @Column(name = "pixel_count")
  private Long pixelCount;

  @Column(name = "min")
  private Double min;

  @Column(name = "max")
  private Double max;

  @Column(name = "sum")
  private Double sum;

  @Column(name = "mean")
  private Double mean;
}
