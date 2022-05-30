package com.revealprecision.revealserver.messaging.message;

import com.revealprecision.revealserver.enums.PlanStatusEnum;
import java.time.LocalDate;
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
public class PlanEvent extends Message {

  private UUID identifier;

  private String name;

  private String title;

  private LocalDate date;

  private LocalDate effectivePeriodStart;

  private LocalDate effectivePeriodEnd;

  private LocationHierarchyEvent locationHierarchy;

  private PlanStatusEnum status;

  private LookupInterventionTypeEvent interventionType;

}