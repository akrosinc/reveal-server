package com.revealprecision.revealserver.messaging.message;

import com.revealprecision.revealserver.api.v1.facade.models.EventFacade;
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
public class FormCaptureEvent extends Message {
  EventFacade rawFormEvent;
  UUID savedEventId;
  UUID planId;
  UUID locationId;
  UUID taskId;
}


