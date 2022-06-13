package com.revealprecision.revealserver.messaging.message;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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
public class LocationPersonBusinessStateAggregate extends Message{

  private UUID locationId;

  private Map<UUID, PersonBusinessStatus> personBusinessStatusMap = new HashMap<>();

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class PersonBusinessStatus extends Message{

    private LocalDateTime updateDateTime;
    private String status;
    private String updatingTaskType;

  }

}
