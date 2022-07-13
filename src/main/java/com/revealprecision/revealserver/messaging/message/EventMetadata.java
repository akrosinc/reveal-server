package com.revealprecision.revealserver.messaging.message;

import com.revealprecision.revealserver.api.v1.facade.models.Obs;
import com.revealprecision.revealserver.enums.EntityPropertiesEnum;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventMetadata extends Message {

  private Obs obs;
  private UUID eventId;
  private UUID baseEntityId;
  private EntityPropertiesEnum entityPropertiesEnum;
  private UUID planIdentifier;
  private UUID taskIdentifier;
  private String user;
  private String dataType;
  private String tag;
  private String eventType;
  private List<Obs> fullObs;
}
