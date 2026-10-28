package com.revealprecision.revealserver.persistence.domain;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.time.LocalDate;
import java.util.Map;
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
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
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
@Table(name = "location_stats")
@SQLDelete(sql = "UPDATE location_stats SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class LocationStats extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  @Column(name = "dataset_id", nullable = false)
  private String datasetId;

  @Column(name = "tag")
  private String tag;

  @Column(name = "capture_date")
  private LocalDate captureDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "location_identifier", referencedColumnName = "identifier", nullable = false)
  private Location location;

  @Column(name = "item_count")
  private Long itemCount;

  @Column(name = "min")
  private Double min;

  @Column(name = "max")
  private Double max;

  @Column(name = "sum")
  private Double sum;

  @Column(name = "mean")
  private Double mean;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> metadata;
}
