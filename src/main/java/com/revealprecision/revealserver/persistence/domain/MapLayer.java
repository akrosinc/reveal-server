package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.enums.EntityStatus;
import com.revealprecision.revealserver.enums.LayerType;
import com.revealprecision.revealserver.model.GeoEnvelope;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
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
@Table(name = "map_layer")
@SQLDelete(sql = "UPDATE map_layer SET entity_status = 'DELETED' where id=?")
@Where(clause = "entity_status='ACTIVE' or entity_status='CREATING'")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class MapLayer extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(name = "layer_identifier", nullable = false)
  private String layerIdentifier;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private LayerType type;

  @Type(type = "jsonb")
  @Column(name = "extent", columnDefinition = "jsonb")
  private GeoEnvelope extent;
}
