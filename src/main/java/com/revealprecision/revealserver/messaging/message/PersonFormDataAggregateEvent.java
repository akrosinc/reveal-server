package com.revealprecision.revealserver.messaging.message;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PersonFormDataAggregateEvent extends Message {

  private UUID identifier;

  private UUID entityId;

  private UUID ancestorNode;

  private UUID hierarchyIdentifier;

  private UUID thisLocation;

  private UUID plan;

  private String tag;

  private String tagValue;

  private String previousTagValue;

  private Integer tagIntegerValue;

  private Integer previousTagIntegerValue;

  private Long increment;

  private Long averageCounter;

}
