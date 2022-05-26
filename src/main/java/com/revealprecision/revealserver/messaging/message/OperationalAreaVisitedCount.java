package com.revealprecision.revealserver.messaging.message;

import java.util.HashMap;
import java.util.Map;
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
public class OperationalAreaVisitedCount extends Message {
 private Map<UUID, IndividualOperationalAreaCountsByBusinessStatus> operationalObj = new HashMap<>();
 private Long operationalAreaVisitedCount;

 @Setter
 @Getter
 @Data
 @AllArgsConstructor
 @NoArgsConstructor
 public static class IndividualOperationalAreaCountsByBusinessStatus {

  Map<String,Long> counts = new HashMap<>();
  boolean operationalAreaIsVisited = false;
 }
}
