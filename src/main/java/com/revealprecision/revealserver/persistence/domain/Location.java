package com.revealprecision.revealserver.persistence.domain;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Getter
@Setter
@RequiredArgsConstructor
@SQLDelete(sql = "UPDATE location SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "geographic_level_id", insertable = false)
  private GeographicLevel geographicLevel;
}
