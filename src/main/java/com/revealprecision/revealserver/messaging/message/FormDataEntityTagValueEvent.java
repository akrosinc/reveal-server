package com.revealprecision.revealserver.messaging.message;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class FormDataEntityTagValueEvent extends Message {

  private EntityTagEvent entityTagEvent;
  private Object value;
  private FormFieldEvent selectedformField;

  private UUID planIdentifier;
  private UUID locationHierarchyIdentifier;
  private UUID entityIdentifier;
  private String geographicLevelName;
  private UUID ancestor;
  private String dateForScopeDate;

  private String supervisor;
  private String cddName;

}
