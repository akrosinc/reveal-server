package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.api.v1.dto.request.ActionRequest;
import com.revealprecision.revealserver.enums.ActionTypeEnum;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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

  private LocalDate timingPeriodStart;

  private LocalDate timingPeriodEnd;

  @ManyToOne
  @JoinColumn(name = "goal_identifier")
  private Goal goal;

  private ActionTypeEnum type;

  @ManyToOne
  @JoinColumn(name = "form_identifier")
  private Form form;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "action")
  private Set<Task> tasks;


  @OneToMany(mappedBy = "action", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
  private Set<Condition> conditions;

  @ManyToOne
  @JoinColumn(name = "lookup_entity_type_identifier")
  private LookupEntityType lookupEntityType;

  public Action update(ActionRequest actionRequest, Form form) {
    this.title = actionRequest.getTitle();
    this.description = actionRequest.getDescription();
    this.timingPeriodStart = actionRequest.getTimingPeriod().getStart();
    this.timingPeriodEnd = actionRequest.getTimingPeriod().getEnd();
    this.form = form;
    return this;
  }
}
