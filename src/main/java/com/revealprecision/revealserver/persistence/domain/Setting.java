package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.api.v1.dto.request.SettingRequest;
import com.revealprecision.revealserver.persistence.generator.SettingServerVersionGenerator;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.List;
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
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.GeneratorType;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
@SQLDelete(sql = "UPDATE setting SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class Setting extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;
  private String settingIdentifier;
  private String type;
  private String key;
  private String value;
  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private List<Object> values;
  private String label;
  private String description;

  @GeneratorType(type = SettingServerVersionGenerator.class, when = GenerationTime.ALWAYS)
  private Long serverVersion;


  public Setting update(SettingRequest settingRequest) {
    this.settingIdentifier = settingRequest.getIdentifier();
    this.type = settingRequest.getType();
    this.key = settingRequest.getKey();
    this.value = settingRequest.getValue();
    this.values = settingRequest.getValues();
    this.label = settingRequest.getLabel();
    this.description = settingRequest.getDescription();
    return this;
  }
}
