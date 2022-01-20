package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.api.v1.dto.request.ActionRequest;
import com.revealprecision.revealserver.enums.ActionSubjectEnum;
import com.revealprecision.revealserver.enums.ActionTypeEnum;
import com.revealprecision.revealserver.enums.LookupUtil;
import java.time.LocalDate;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
import org.hibernate.annotations.SQLDelete;
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
public class Action extends AbstractAuditableEntity {

  @Id
  @GeneratedValue
  private UUID identifier;

  private String title;

  private String description;

  private String code;

  private LocalDate timingPeriodStart;

  private LocalDate timingPeriodEnd;

  private String reason;

  @Enumerated(EnumType.STRING)
  private ActionSubjectEnum subject;

  @ManyToOne
  @JoinColumn(name = "goal_identifier")
  private Goal goal;

  private ActionTypeEnum type;

  private String definitionUri;

  public Action update(ActionRequest actionRequest) {
    this.title = actionRequest.getTitle();
    this.description = actionRequest.getDescription();
    this.timingPeriodStart = actionRequest.getTimingPeriod().getStart();
    this.timingPeriodEnd = actionRequest.getTimingPeriod().getEnd();
    this.subject = LookupUtil.lookup(ActionSubjectEnum.class,
        actionRequest.getSubjectCodableConcept().getText());
    this.definitionUri = actionRequest.getDefinitionUri();
    this.type = actionRequest.getType();
    return this;
  }
}
