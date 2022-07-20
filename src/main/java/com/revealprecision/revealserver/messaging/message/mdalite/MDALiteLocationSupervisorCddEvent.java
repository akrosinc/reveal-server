package com.revealprecision.revealserver.messaging.message.mdalite;

import com.revealprecision.revealserver.messaging.message.Message;
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
public class MDALiteLocationSupervisorCddEvent extends Message {

  private UUID locationIdentifier;

  private UUID planIdentifier;

  private UUID locationHierarchyIdentifier;

  private String supervisorName;

  private String cddName;

}
