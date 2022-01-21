package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.api.v1.dto.request.GoalUpdateRequest;
import com.revealprecision.revealserver.enums.PriorityEnum;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

@FieldNameConstants
@Entity
@Audited
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "UPDATE goal SET entity_status = 'DELETED' where identifier=?")
@Where(clause = "entity_status='ACTIVE'")
@ToString
public class Goal extends AbstractAuditableEntity {

  @Id
  private String identifier;

  private String description;

  @Enumerated(EnumType.STRING)
  private PriorityEnum priority;

  @ManyToOne
  @JoinColumn(name = "plan_identifier", nullable = false)
  private Plan plan;

  @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL)
  private Set<Target> targets;

  @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL)
  private Set<Action> actions;

  public Goal update(GoalUpdateRequest request) {
    this.description = request.getDescription();
    this.priority = request.getPriority();
    return this;
  }
}
