package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.revealprecision.revealserver.persistence.generator.EventServerVersionGenerator;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE action SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class Event extends AbstractAuditableEntity {

  @Id
  @GeneratedValue(generator = "custom-generator")
  @GenericGenerator(name = "custom-generator", strategy = "com.revealprecision.revealserver.persistence.generator.CustomIdentifierGenerator")
  private UUID identifier;

  private String eventType;

  private UUID taskIdentifier;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "user_identifier", referencedColumnName = "identifier")
  private User user;

  private LocalDateTime captureDatetime;

  @ManyToOne
  @JoinColumn(name = "organization_identifier", referencedColumnName = "identifier")
  private Organization organization;

  private UUID planIdentifier;

  private UUID locationIdentifier;

  private UUID baseEntityIdentifier;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private JsonNode additionalInformation; //We are storing raw json event data here.

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private JsonNode details;

  @GeneratorType(type = EventServerVersionGenerator.class, when = GenerationTime.ALWAYS)
  private Long serverVersion;
}
