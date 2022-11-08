package com.revealprecision.revealserver.persistence.domain;

import com.vladmihalcea.hibernate.type.array.ListArrayType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@FieldNameConstants
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeDef(
    name = "list-array",
    typeClass = ListArrayType.class
)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Accessors(chain = true, fluent = true)
public class EventTracker {

  @Id
  @GeneratedValue(generator = "custom-generator")
  @GenericGenerator(name = "custom-generator", strategy = "com.revealprecision.revealserver.persistence.generator.CustomIdentifierGenerator")
  private UUID identifier;
  private String aggregationKey;
  private UUID locationIdentifier;
  private UUID planIdentifier;
  private String eventType;
  private UUID taskIdentifier;
  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private Map<String, List<Object>> observations;
  private String supervisor;
  private String deviceUser;
  private String operationDatetime;
  @Type(type = "list-array")
  private List<UUID> contributingEvents;

}
