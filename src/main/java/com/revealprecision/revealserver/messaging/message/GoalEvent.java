package com.revealprecision.revealserver.messaging.message;

import com.revealprecision.revealserver.enums.PriorityEnum;
import com.revealprecision.revealserver.persistence.domain.AbstractAuditableEntity;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GoalEvent extends AbstractAuditableEntity {

  private UUID identifier;

  private String description;

  private PriorityEnum priority;

  private PlanEvent plan;

}
