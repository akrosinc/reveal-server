package com.revealprecision.revealserver.messaging.message;

import java.util.List;
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
public class PlanLocationAssignMessage extends Message {

  private UUID planIdentifier;

  private List<String> locationsAdded;

  private List<String> locationsRemoved;

  private String ownerId;

  int deleteByPlan;
  int saveAll;
  int deleteByPlanAndLocation;

}
