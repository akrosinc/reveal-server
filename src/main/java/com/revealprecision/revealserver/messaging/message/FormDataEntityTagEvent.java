package com.revealprecision.revealserver.messaging.message;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class FormDataEntityTagEvent extends Message {

  private String eventType;
  private String eventId;
  private List<FormDataEntityTagValueEvent> formDataEntityTagValueEvents;
  private UUID taskIdentifier;
  private UUID planIdentifier;
  private UUID entityId;
  private UUID locationHierarchyIdentifier;
  private UUID locationIdentifier;
  private String geographicalLevelName;
  private String date;
  private String user;

  private String supervisor;
  private String cddName;
  private UUID formSubmissionId;

  private String additionalKeyString;
}
