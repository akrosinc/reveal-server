package com.revealprecision.revealserver.messaging.message;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class FormDataEntityTagValueEvent extends Message{
  private EntityTagEvent entityTagEvent;
  private Object value;
  private String formField;

  private UUID planIdentifier;
  private UUID locationHierarchyIdentifier;
  private UUID locationIdentifier;
  private String geographicLevelName;
  private UUID ancestor;
  private String dateForScopeDate;

  private String supervisor;
  private String cddName;

}
