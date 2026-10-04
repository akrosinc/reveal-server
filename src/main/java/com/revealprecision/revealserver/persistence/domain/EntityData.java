package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

/**
 * Maps the {@code entity_data} table. This table holds arbitrary, location-linked entities (for
 * example emanators) that a task can be generated against. It is intentionally a plain (non
 * audited) entity because the underlying table carries no audit columns.
 */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class EntityData {

  @Id
  private UUID identifier;

  @Column(nullable = false)
  private String name;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private JsonNode data;

  @Type(type = "jsonb")
  @Column(name = "entity_schema", columnDefinition = "jsonb")
  private JsonNode entitySchema;

  @Column(name = "location_identifier")
  private UUID locationIdentifier;
}
