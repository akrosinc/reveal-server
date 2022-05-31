package com.revealprecision.revealserver.messaging.message;

import com.revealprecision.revealserver.enums.ActionTypeEnum;
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
public class ActionEvent extends Message {

  private UUID identifier;

  private String title;

  private String description;

  private LocalDate timingPeriodStart;

  private LocalDate timingPeriodEnd;

  private GoalEvent goal;

  private ActionTypeEnum type;

  private LookupEntityTypeEvent lookupEntityType;

}
