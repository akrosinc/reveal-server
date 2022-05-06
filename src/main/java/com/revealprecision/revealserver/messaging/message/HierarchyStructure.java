package com.revealprecision.revealserver.messaging.message;

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
public class HierarchyStructure extends Message{

  private String structureId;

  private String parentId;

  private boolean isAssigned;

  private String planIdentifier;

}
