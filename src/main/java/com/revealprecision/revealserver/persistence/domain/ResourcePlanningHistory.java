package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.api.v1.dto.request.ResourcePlanningDashboardRequest;
import com.revealprecision.revealserver.enums.EntityStatus;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
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
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Where(clause = "entity_status='ACTIVE'")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@FieldNameConstants
public class ResourcePlanningHistory extends AbstractAuditableEntity{

  @Id
  @GeneratedValue
  private UUID identifier;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private ResourcePlanningDashboardRequest history;

  private String name;

  public ResourcePlanningHistory(ResourcePlanningDashboardRequest history, String name){
    this.history = history;
    this.name = name;
    this.setEntityStatus(EntityStatus.ACTIVE);
  }
}
