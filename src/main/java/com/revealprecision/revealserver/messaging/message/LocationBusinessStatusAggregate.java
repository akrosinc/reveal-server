package com.revealprecision.revealserver.messaging.message;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Setter
@Getter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationBusinessStatusAggregate extends Message {


  private UUID entityId;
  private String businessStatus;
  private LocalDateTime updateTime;
  private List<UUID> ancestry;

}
