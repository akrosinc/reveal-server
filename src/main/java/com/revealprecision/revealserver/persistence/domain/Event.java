package com.revealprecision.revealserver.persistence.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID identifier;

  private String name;

  private String eventType;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "form_data_identifier", referencedColumnName = "identifier")
  private FormData formData;

  @ManyToOne
  @JoinColumn(name = "task_identifier", referencedColumnName = "identifier")
  private Task task;

  private UUID userIdentifier;

  private LocalDateTime captureDate;

  @ManyToOne
  @JoinColumn(name = "organization_identifier", referencedColumnName = "identifier")
  private Organization organization;

  @ManyToOne
  @JoinColumn(name = "plan_identifier", referencedColumnName = "identifier")
  private Plan plan;

  @ManyToOne
  @JoinColumn(name = "location_identifier", referencedColumnName = "identifier")
  private Location location;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private JsonNode additionalInformation;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private JsonNode details;

  private int version;

}
