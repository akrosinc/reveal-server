package com.revealprecision.revealserver.messaging.message;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class FormDataEntityTagEvent extends Message{
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
}
